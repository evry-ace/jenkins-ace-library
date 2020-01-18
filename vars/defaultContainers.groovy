Map call() {
  String cdImg = 'evryace/helm-kubectl-terraform:v3.0.1__v1.13.10__0.12.18'

  containers = [
    kubectl: cdImg,
    helm: cdImg,
    terraform: cdImg,
    cd: cdImg,
    ace: 'evryace/ace-2-values:14',
  ]
}
