Object call(Map opts = [:]) {
  String image = opts.img ?: defaultContainers().cd
  return containerTemplate(name: 'cd', image: image, command: 'cat', ttyEnabled: true)
}
