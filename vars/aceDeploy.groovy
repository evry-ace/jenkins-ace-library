#!/usr/bin/env groovy

import no.ace.Config

def call(config, envName, opts = [:]) {
  def acefile = opts.acefile ?: 'ace.yaml'
  def debug = opts.containsKey('debug') ? opts.debug : true
  def dryrun = opts.dryrun ?: false
  def wait = opts.containsKey('wait') ? opts.wait : true
  def timeout = opts.timeout ?: 600
  def dockerSet = opts.containsKey('dockerSet') ? opts.dockerSet : true

  def kubectlImage = opts.kubectlImage ?: 'lachlanevenson/k8s-kubectl'
  def kubectlVersion = opts.kubectlVersion ?: 'v1.6.0'
  def kubectlOpts = opts.kubectlOpts ?: "--entrypoint=''"

  def helmImage = opts.helmImage ?: 'lachlanevenson/k8s-helm'
  def helmVersion = opts.helmVersion ?: 'v2.6.0'
  def helmOpts = opts.helmOpts ?: "--entrypoint=''"
  def helmValuesFile = '.ace/values.yaml'

  def ace = Config.parse(config, envName)
  ace.helm = ace.helm ?: [:]
  ace.helm.values = ace.helm.values ?: [:]

  if (dockerSet) {
    ace.helm.values.image = ace.helm.values.image ?: [:]

    if (ace.helm.image) {
      def (repository, tag) = ace.helm.image.split(':')
      ace.helm.values.image.repository = repository
      ace.helm.values.image.tag = tag
    }

    if (ace.helm.registry) {
      ace.helm.values.image.repository = [
        ace.helm.registry,
        ace.helm.values.image.repository
      ].join('/')

      ace.helm.values.image.pullSecrets = ace.helm.values.image.pullSecrets ?: []
      ace.helm.values.image.pullSecrets.push(ace.helm.registry)
    }
  }

  if (!ace.helm.name) {
    ace.helm.name = env.JOB_BASE_NAME
    ace.helm.nameEnvify = true
  }

  def helmName = ace.helm.nameEnvify ? "${ace.helm.name}-${envName}" : ace.helm.name
  def helmNamespace = ace.helm.namespace
  def helmRepo = ace.helm.repo
  def helmRepoName = ace.helm.repoName
  def helmChart = ace.helm.chart
  def helmChartVersion = ace.helm.version

  println ace.helm.values
  println "Writing ace.helm.values to ${helmValuesFile}..."
  writeYaml file: helmValuesFile, data: ace.helm.values

  def credId  = ace.helm.cluster
  def credVar = 'KUBECONFIG'

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
      def helmExists = sh(script: "helm history ${helmName}", returnStatus: true) == 0

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
          } catch (e) {}
        }

        throw err
      }
    }
  }
}
