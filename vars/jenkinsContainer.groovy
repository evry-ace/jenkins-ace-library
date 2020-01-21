Object call(Map opts = [:]) {
  String image = opts.img ?: 'jenkins/jnlp-slave:alpine'

  return containerTemplate(
    name: 'jenkins',
    image: image,
    command: 'cat',
    ttyEnabled: true
  )
}
