#!/usr/bin/env groovy

import no.ace.Config

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
  String kubectlOpts = opts.kubectlOpts ?: ["--entrypoint=''"]

  String helmContainer = containers.helm ?: ''
  String helmOpts = opts.helmOpts ?: ["--entrypoint=''"]
  String helmValuesFile = '.ace/values.yaml'

<<<<<<< HEAD
  String extraParams = opts.extraParams ?: ""
  
  def (String org, String repo, String branch) = env.JOB_NAME.split('/')
  println "org=${org}, repo=${repo}, branch=${branch}"
=======
  println "[ace] Job name: ${env.JOB_NAME}"

  List<String> nameParts = env.JOB_NAME.split('/')
  def (String org, String jobName, String branch) = nameParts.size() == 2 ? [null] + nameParts : nameParts
  println "[ace] org=${org}, repo=${jobName}, branch=${branch}"
>>>>>>> deploy: Support using alt container

  // @TODO this logic could be moved to the Config Class
  config.name = config.name ?: jobName

  Map ace = Config.parse(config, envName)
  ace.helm = ace.helm ?: [:]
  ace.helm.values = ace.helm.values ?: [:]

  println "[ace] Configuration done."

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
      ace.helm.values.image.pullSecrets.push(ace.helm.registry)
    }
  }

  String helmName = ace.helm.name
  String helmNamespace = ace.helm.namespace
  String helmRepo = ace.helm.repo
  String helmRepoName = ace.helm.repoName
  String helmChart = ace.helm.chart
  String helmChartVersion = ace.helm.version

  println "[ace] Writing values '${ace.helm.values}' -> ${helmValuesFile}"

  Boolean valuesFileExists = fileExists(helmValuesFile)
  if (valuesFileExists) {
    sh "rm ${helmValuesFile}"
  }

  writeYaml file: helmValuesFile, data: ace.helm.values

  println "[ace] Wrote values ${helmValuesFile}"

  String credId  = ace.helm.cluster
  String credVar = 'KUBECONFIG'

  withCredentials([file(credentialsId: credId, variable: credVar)]) {
    // Get Helm Version
    // @TODO this will not work properly due to the image name
    aceContainer(kubectlContainer, kubectlOpts, [:]) {
      script = '''
        kubectl get pod -n kube-system -l app=helm,name=tiller \
          -o jsonpath="{ .items[0].spec.containers[0].image }" | cut -d ':' -f2
      '''
      helmVersion = sh(script: script, returnStdout: true)?.trim()

      println "[ace] Helm version discovered: ${helmVersion}"
    }

    // Deploy Helm Release
    // @TODO this will not work properly due to the image name
    aceContainer(helmContainer, helmOpts, [:]) {
      sh """
        set -u
        set -e

        env

        # Set Helm Home
        export HELM_HOME=\$(pwd)

        # Install Helm locally
        helm init -c

        # Check Helm connection
        helm version
      """

      // Check if release exists already. This is due to a suddle bug in Helm
      // where a failed first deploy (release) will prevent any further release
      // for the same release name. We have solved this by purging the failed
      // release if we detect a failure.
      Boolean helmExists = sh(script: "helm history ${helmName}", returnStatus: true) == 0

      try {
        sh """
          set -u
          set -e

          # Set Helm Home
          export HELM_HOME=\$(pwd)

          # Add Helm repository
          helm repo add ${helmRepoName} ${helmRepo}
          helm repo update

          helm upgrade --install \
            --namespace ${helmNamespace} \
            -f ${helmValuesFile} \
            --debug=${debug} \
            --dry-run=${dryrun} \
            --wait=${wait} \
            --timeout=${timeout} \
            --version=${helmChartVersion} \
            ${extraParams} \
            ${helmName} \
            ${helmChart}
        """
      } catch (err) {
        if (!helmExists && !dryrun) {
          try {
            sh(script: "helm delete ${helmName} --purge", returnStatus: true)
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
