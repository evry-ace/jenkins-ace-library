#!/usr/bin/env groovy

import no.ace.AceUtils

def call(global, ace, environment, opts = [:]) {
  def debug = opts.containsKey('debug') ? opts.debug : true
  def dryrun = opts.dryrun ?: false
  def wait = opts.containsKey('wait') ? opts.wait : true
  def timeout = opts.timeout ?: 600
  def dockerSet = opts.containsKey('dockerSet') ? opts.dockerSet : true

  def kubectlVersion = opts.kubectlVersion ?: 'v1.6.0'
  def helmVersion = opts.helmVersion ?: 'v2.6.0'

  // Instanciate AceUtils with global configuration
  def utils = new AceUtils(global)

  // Dereive Kuberentes Cluster from Environment
  def cluster = utils.envCluster(environment)

  // Make a copy of input values to prevent reuse of value HashMap in Jenkins
  writeYaml file: '.helmdeploytemp.yaml', data: ace
  def values = readYaml file: '.helmdeploytemp.yaml'

  // Path to Helm configuration values
  def helmPath = values.helm?.path ?: 'deploy'

  // Name of Helm release
  def helmName = values.name ? "${values.name}-${environment}" : ''
  if (helmName == '') { throw new IllegalArgumentException("name can not be empty") }

  // Name of Helm chart
  // https://github.com/evry-ace/helm-charts
  def helmChart = values.helm.chart ?: ''
  if (helmChart == '') { throw new IllegalArgumentException("helmChart can not be empty") }

  // Helm Chart Repo
  // https://github.com/evry-ace/helm-charts
  def helmRepo = values.helm.repo ?: 'https://evry-ace.github.io/helm-charts'
  def helmRepoName = values.helm.repoName ?: 'ace'

  // Valid version of chart
  // Can be a version "range" such as "^1.0.0"
  def chartVersion = values.helm.version ?: ''
  if (chartVersion == '') { throw new IllegalArgumentException("chartVersion can not be empty") }

  // Docker Image to inject to Helm chart
  def dockerImage = values.dockerImageName ?: values.helm?.default?.image?.image ?: ''
  if (dockerSet && dockerImage == '') {
    throw new IllegalArgumentException("dockerImage can not be empty")
  }

  def dockerRegistry = dockerSet ? cluster.registry : ''

  // Kubernetes namespace for Helm chart
  if (!values.namespace) { throw new IllegalArgumentException("namespace can not be empty") }
  namespace = utils.envNamespace(environment, values.namespace, values.namespaceEnvify)

  /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * Default Helm Configurations
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
  values.helm.default = values.helm.default ?: [:]

  // Docker Image
  if (dockerImage) {
    values.helm.default.image = values.helm.default.image ?: [:]
    values.helm.default.image.image = dockerImage ?: values.helm.default.image.image
  }

  // Docker Registry
  if (dockerRegistry) {
    values.helm.default.dockerRegistry = dockerRegistry
  }

  // Helm Release Name
  values.helm.default.name = values.helm.default.name ?: values.name

  // Helm Metadata
  values.helm.default.meta = values.helm.default.meta ?: [:]

  // Contact Metadata
  values.helm.default.meta.contact = values.helm.default.meta.contact ?: values.contact ?: [:]

  // Git Metadata
  values.helm.default.meta.git = [
    git_commit: gitCommit(),
    git_url: gitUrl(),
    branch_name: env.branch_name,
  ]

  // Jenkins Metadata
  values.helm.default.meta.jenkins = [
    job_name: env.job_name,
    build_number: env.build_number,
    build_url: env.build_url,
  ]

  // Kubernetes Secrets
  if (values.helm.default.secrets) {
    for (secret in values.helm.default.secrets) {
      secret.valueFrom.secretKeyRef.remove('type')
      secret.valueFrom.secretKeyRef.remove('desc')
    }
  }

  // ConfigMap Parsing
  if (values.helm.default.configFiles) {
    values.helm.default.configFiles = parseConfigFiles(values.helm.default.configFiles)
  }

  /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * Environment Specific Configurations
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
  values.deploy = values.deploy ?: [:]

  def deploy = values.deploy[environment] ?: [:]

  // Allow Kubernetes Namespace from Environment
  namespace = deploy.namespace ?: namespace

  deploy.values = deploy.values ?: [:]

  // Set App Environment
  deploy.values.appEnv = deploy.values.appEnv ?: environment

  // Set Env Ingress
  deploy.values.ingress = utils.helmIngress(values.name, environment, deploy.values.ingress, values.helm.default.ingress)
  values.helm.default.remove('ingress')

  // Set Env Vars
  deploy.values.environment = (deploy.values.environment ?: []) + (values.helm.default.environment ?: [])
  values.helm.default.remove('environment')

  // ConfigMap Parsing
  if (deploy.values.configFiles) {
    deploy.values.configFiles = parseConfigFiles(deploy.values.configFiles)
  }

  if (debug) {
    println "writing ${helmPath}/default.yaml..."
    println values.helm.default

    println "writing ${helmPath}/${environment}.yaml..."
    println deploy.values
  }

  writeYaml file: "${helmPath}/default.yaml", data: values.helm.default
  writeYaml file: "${helmPath}/${environment}.yaml", data: deploy.values

  def credId  = cluster.credential
  def credVar = 'KUBECONFIG'

  // Deploy to specified cluter environment
  withCredentials([file(credentialsId: credId, variable: credVar)]) {
    docker.image("lachlanevenson/k8s-kubectl:${kubectlVersion}").inside() {
      script = '''
        kubectl get deploy -n kube-system -o wide \
          | grep tiller \
          | awk '{split($8,a,":"); print a[2]}'
      '''
      helmVersion = sh(script: script, returnStdout: true)?.trim()

      println "Helm version discovered: ${helmVersion}"
    }

    docker.image("lachlanevenson/k8s-helm:${helmVersion}").inside() {
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
            --namespace ${namespace} \
            -f ${helmPath}/default.yaml \
            -f ${helmPath}/${environment}.yaml \
            --debug=${debug} \
            --dry-run=${dryrun} \
            --wait=${wait} \
            --timeout=${timeout} \
            --version=${chartVersion} \
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
