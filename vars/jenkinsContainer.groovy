Object call(Map opts = [:]) {
  String image = opts.img ?: defaultContainers().jenkins

  return containerTemplate(
    name: 'jenkins',
    image: image,
    command: 'cat',
    ttyEnabled: true
  )
}
