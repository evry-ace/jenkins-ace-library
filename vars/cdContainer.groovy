Object call(Map opts = [:]) {
  String image = opts.img ?: 'evryace/helm-kubectl-terraform:v3.0.2__v1.13.10__0.12.19'
  return containerTemplate(name: 'cd', image: image, command: 'cat', ttyEnabled: true)
}
