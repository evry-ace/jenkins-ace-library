void call(Map opts = [:]) {
  String image = opts.image ?: 'gcr.io/kaniko-project/executor:debug'

  return containerTemplate(
    name: 'kaniko',
    image: image,
    command: '/busybox/cat',
    ttyEnabled: true)
}
