Object call(Map opts = [:]) {
  String image = opts.img ?: defaultContainers().ace
  return containerTemplate(name: 'ace', image: image, command: 'cat', ttyEnabled: true)
}
