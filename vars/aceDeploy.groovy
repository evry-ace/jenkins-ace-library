#!/usr/bin/env groovy

import no.ace.Config

List<Object> getParts(String name) {
  List<String> parts = name.split('/')
  return parts.size() == 2 ? [null] + parts : parts
}

@SuppressWarnings(['MethodSize', 'CyclomaticComplexity'])
void call(Map config, String envName, Map opts = [:]) {
  print "[ace] Deplying to ${envName}"

  // String  acefile = opts.acefile ?: 'ace.yaml'
  Boolean debug = opts.containsKey('debug') ? opts.debug : true
  Boolean dryrun = opts.dryrun ?: false
  Boolean wait = opts.containsKey('wait') ? opts.wait : true
  Integer timeout = opts.timeout ?: 600

  Boolean dockerSet = opts.containsKey('dockerSet') ? opts.dockerSet : true

  Map containers = opts.containers ?: [:]
  String kubectlContainer = containers.kubectl ?: ''
  List<String> kubectlOpts = opts.kubectlOpts ?: ["--entrypoint=''"]

  String helmContainer = containers.helm ?: ''
  List<String> helmOpts = opts.helmOpts ?: ["--entrypoint=''"]
  String helmValuesFile = '.ace/values.yaml'

  String extraParams = opts.extraParams ?: ''

  println "[ace] Got containers ${containers}"

  println "[ace] Job name: ${env.JOB_NAME}"

  def (String org, String jobName, String branch) = getParts(env.JOB_NAME)
  println "[ace] org=${org}, repo=${jobName}, branch=${branch}"

  // @TODO this logic could be moved to the Config Class
  config.name = config.name ?: jobName

  Map ace = Config.parse(config, envName)
  ace.helm = ace.helm ?: [:]
  ace.helm.values = ace.helm.values ?: [:]

  println "[ace] Name: ${config.name}"
  println '[ace] Configuration done.'

  if (dockerSet) {
    ace.helm.values.image = ace.helm.values.image ?: [:]

    if (ace.helm.image) {
      def (String repository, String tag) = ace.helm.image.split(':')
      ace.helm.values.image.repository = repository
      ace.helm.values.image.tag = tag
    }

    if (ace.helm.registry) {
      ace.helm.values.image.repository = [
        ace.helm.registry,
        ace.helm.values.image.repository,
      ].join('/')

      ace.helm.values.image.pullSecrets = ace.helm.values.image.pullSecrets ?: []
    }
  }

  String helmName = ace.helm.name
  String helmNamespace = ace.helm.namespace
  String helmRepo = ace.helm.repo
  String helmRepoName = ace.helm.repoName
  String helmChart = ace.helm.chart
  String helmChartVersion = ace.helm.version
  // String helmDiscoverVersion = opts.helmDiscoverVersion ?: true

  println "[ace] Writing values '${ace.helm.values}' -> ${helmValuesFile}"

  Boolean valuesFileExists = fileExists(helmValuesFile)
  if (valuesFileExists) {
    sh "rm ${helmValuesFile}"
  }

  writeYaml file: helmValuesFile, data: ace.helm.values

  println "[ace] Wrote values to ${helmValuesFile}"
  println "[ace] Values are: ${readFile helmValuesFile}"

  String credId = opts.k8sConfigCredId ?: ace.helm.cluster
  Map credsOpts = [k8sConfigCredId: credId]

  credsWrap(credsOpts) {
    // Get Helm Version
    // @TODO this will not work properly due to the image name
    if (!opts.containers.helm) {
      aceContainer(kubectlContainer, kubectlOpts, [:]) {
        script = '''
          kubectl get pod -n kube-system -l app=helm,name=tiller \
            -o jsonpath="{ .items[0].spec.containers[0].image }" | cut -d ':' -f2
        '''
        helmVersion = sh(script: script, returnStdout: true)?.trim()

        println "[ace] Helm version discovered: ${helmVersion}"
      }
    }

    // Deploy Helm Release
    // @TODO this will not work properly due to the image name
    aceContainer(helmContainer, helmOpts, [:]) {
      script = 'helm version -c 2>&1 | grep "v2\\." > .ace/helmver'
      sh(script: script, returnStatus: true)
      String thisHelmVersion = readFile('.ace/helmver')

      Boolean helmIsV3 = thisHelmVersion == ''
      helmIsV3Str = helmIsV3.toString()
      if (helmIsV3) {
        print '[ace] Hurray, Helm 3 detected!'
      } else {
        sh """
          set -u
          set -e

          env

          # Set Helm Home
          export HELM_HOME=\$(pwd)/helm
          export XDG_CACHE_HOME=\$HELM_HOME/cache
          export XDG_CONFIG_HOME=\$HELM_HOME/config
          export XDG_data_HOME=\$HELM_HOME/data

          # Install Helm locally and check connection if v2
          [ "${helmIsV3Str}" == "false" ] && {
            helm init -c
            helm version -s
          }
        """
      }

      // Check if release exists already. This is due to a suddle bug in Helm
      // where a failed first deploy (release) will prevent any further release
      // for the same release name. We have solved this by purging the failed
      // release if we detect a failure.
      println '[ace] Looking for previous histories.'
      existsArgs = helmIsV3 ? ['--namespace', "${helmNamespace}"] : []
      Int helmExistsStatus = sh(
        script: "helm history ${existsArgs.join(' ')} ${helmName}",
        returnStatus: true
      )
      Boolean helmExists = helmExistsStatus == 0
      println "[ace] Release exists: ${helmExists}."

      timeoutAsStr = helmIsV3 ? "${timeout}s" : "${timeout}"
      try {
        sh """
          set -u
          set -e

          # Set Helm Home
          export HELM_HOME=\$(pwd)
          export XDG_CACHE_HOME=\$HELM_HOME/cache
          export XDG_CONFIG_HOME=\$HELM_HOME/config
          export XDG_data_HOME=\$HELM_HOME/data

          # Add Helm repository
          helm repo add ${helmRepoName} ${helmRepo}
          helm repo update

          helm upgrade --install \
            --namespace ${helmNamespace} \
            -f ${helmValuesFile} \
            --debug=${debug} \
            --dry-run=${dryrun} \
            --wait=${wait} \
            --timeout=${timeoutAsStr} \
            --version=${helmChartVersion} \
            ${extraParams} \
            ${helmName} \
            ${helmChart}
        """
      } catch (err) {
        if (!helmExists && !dryrun) {
          try {
            String deleteArgs = [
              helmName,
            ]

            if (helmIsV3) {
              deleteArgs = deleteArgs + ["--namespace=${helmNamespace}"]
            } else {
              deleteArgs.add('--purge')
            }

            sh(script: "helm delete ${deleteArgs.join(' ')}" , returnStatus: true)
          } catch (e) {
            println '[ace] Helm purge failed'
            println "[ace] ${e}"
          }
        }

        throw err
      }
    }
  }
}
