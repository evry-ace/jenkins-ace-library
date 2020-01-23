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
  String helmRepo = target.repo
  String helmRepoName = target.repoName
  String helmChart = target.chart
  String helmChartVersion = target.version

  String credId = opts.k8sConfigCredId ?: targetEnv.cluster
  Map credsOpts = [k8sConfigCredId: credId]

  credsWrap(credsOpts) {
    // Deploy Helm Release
    // @TODO this will not work properly due to the image name
    aceContainerWrapper(helmContainer, helmOpts, [:]) {
      // Check if release exists already. This is due to a suddle bug in Helm
      // where a failed first deploy (release) will prevent any further release
      // for the same release name. We have solved this by purging the failed
      // release if we detect a failure.
      println '[ace] Looking for previous histories.'
      existsArgs = ['--namespace', "${helmNamespace}"]
      Integer helmExistsStatus = sh(
        script: "helm history ${existsArgs.join(' ')} ${helmName}",
        returnStatus: true
      )
      Boolean helmExists = helmExistsStatus == 0
      println "[ace] Release exists: ${helmExists}."

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
            --timeout=${timeout}s \
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

            deleteArgs = deleteArgs + ["--namespace=${helmNamespace}"]
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
