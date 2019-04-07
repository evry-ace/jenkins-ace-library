#!/usr/bin/env groovy

import no.ace.Config

@SuppressWarnings(['MethodSize', 'CyclomaticComplexity'])
void call(Map config, String envName, Map opts = [:]) {
  // String  acefile = opts.acefile ?: 'ace.yaml'
  Boolean debug = opts.containsKey('debug') ? opts.debug : true
  Boolean dryrun = opts.dryrun ?: false
  Boolean wait = opts.containsKey('wait') ? opts.wait : true
  Integer timeout = opts.timeout ?: 600
  Boolean dockerSet = opts.containsKey('dockerSet') ? opts.dockerSet : true

  String kubectlImage = opts.kubectlImage ?: 'lachlanevenson/k8s-kubectl'
  String kubectlVersion = opts.kubectlVersion ?: 'v1.6.0'
  String kubectlOpts = opts.kubectlOpts ?: "--entrypoint=''"

  String helmImage = opts.helmImage ?: 'lachlanevenson/k8s-helm'
  String helmVersion = opts.helmVersion ?: 'v2.6.0'
  String helmOpts = opts.helmOpts ?: "--entrypoint=''"
  String helmValuesFile = '.ace/values.yaml'

  def (String org, String repo, String branch) = env.JOB_NAME.split('/')
  println "org=${org}, repo=${repo}, branch=${branch}"

  // @TODO this logic could be moved to the Config Class
  config.name = config.name ?: repo

  Map ace = Config.parse(config, envName)
  ace.helm = ace.helm ?: [:]
  ace.helm.values = ace.helm.values ?: [:]

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

  println ace.helm.values
  println "Writing ace.helm.values to ${helmValuesFile}..."

  sh "[ -f ${helmValuesFile} ] && rm ${helmValuesFile}"

  writeYaml file: helmValuesFile, data: ace.helm.values

  String credId  = ace.helm.cluster
  String credVar = 'KUBECONFIG'

  withCredentials([file(credentialsId: credId, variable: credVar)]) {
    // Get Helm Version
    docker.image("${kubectlImage}:${kubectlVersion}").inside(kubectlOpts) {
      script = '''
        kubectl get deploy -n kube-system -o wide \
          | grep tiller \
          | awk '{split($8,a,":"); print a[2]}'
      '''
      helmVersion = sh(script: script, returnStdout: true)?.trim()

      println "Helm version discovered: ${helmVersion}"
    }

    // Deploy Helm Release
    docker.image("${helmImage}:${helmVersion}").inside(helmOpts) {
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
            ${helmName} \
            ${helmChart}
        """
      } catch (err) {
        if (!helmExists && !dryrun) {
          try {
            sh(script: "helm delete ${helmName} --purge", returnStatus: true)
          } catch (e) {
            println 'Helm purge failed'
            println e
          }
        }

        throw err
      }
    }
  }
}
