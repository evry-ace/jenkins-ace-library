Object call(Map opts = [:]) {
  String image = opts.img ?: defaultsContainers().cd
  return containerTemplate(name: 'cd', image: image, command: 'cat', ttyEnabled: true)
}
