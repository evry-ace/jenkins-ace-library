Map call() {
  String cdImg = 'evryace/helm-kubectl-terraform:v3.0.1__v1.13.10__0.12.18'

  containers = [
    kubectl: cdImg,
    helm: cdImg,
    terraform: cdImg,
    cd: cdImg,
    ace: 'evryace/ace-2-values:17',
    jenkins: 'jenkins/jnlp-slave:alpine',
    kaniko: 'gcr.io/kaniko-project/executor:debug-v0.24.0',
  ]

  return containers
}
