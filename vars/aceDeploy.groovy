#!/usr/bin/env groovy

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

  Map containers = opts.containers ?: [:]
  String kubectlContainer = containers.kubectl ?: ''
  List<String> kubectlOpts = opts.kubectlOpts ?: ["--entrypoint=''"]

  String helmContainer = containers.helm ?: ''
  List<String> helmOpts = opts.helmOpts ?: ["--entrypoint=''"]

  String extraParams = opts.extraParams ?: ''

  println "[ace] Got containers ${containers}"
  println "[ace] Job name: ${env.JOB_NAME}"

  def (String org, String jobName, String branch) = getParts(env.JOB_NAME)
  println "[ace] org=${org}, repo=${jobName}, branch=${branch}"

  config.name = config.name ?: jobName

  Map target = readYaml file: 'target-data/target.yaml'
  Map targetEnv = readYaml file: "target-data/target.${envName}.yaml"

  println '[ace] Configuration done.'

  String helmName = target.name
  String helmNamespace = targetEnv.namespace
  String helmRepo = target.helm.repo
  String helmRepoName = target.helm.repoName
  String helmChart = target.chart
  String helmChartVersion = target.version

  String credId = opts.k8sConfigCredId ?: targetEnv.cluster
  Map credsOpts = [k8sConfigCredId: credId]

  credsWrap(credsOpts) {
    // Get Helm Version
    // @TODO this will not work properly due to the image name
    if (!opts.containers.helm) {
      aceContainerWrapper(kubectlContainer, kubectlOpts, [:]) {
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
    aceContainerWrapper(helmContainer, helmOpts, [:]) {
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
      Integer helmExistsStatus = sh(
        script: "helm history ${existsArgs.join(' ')} ${helmName}",
        returnStatus: true
      )
      Boolean helmExists = helmExistsStatus == 0
      println "[ace] Release exists: ${helmExists}."

      timeoutAsStr = helmIsV3 ? "${timeout}s" : "${timeout}"

      extraParams = "${extraParams}"

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
            -f out/values.${envName}.yaml \
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
