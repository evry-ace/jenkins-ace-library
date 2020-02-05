void call(Map opts = [:]) {
  String image = opts.image ?: defaultContainers().kaniko

  return containerTemplate(
    name: 'kaniko',
    image: image,
    command: '/busybox/cat',
    ttyEnabled: true)
}
