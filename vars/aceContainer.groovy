Object call(Map opts = [:]) {
  String image = opts.img ?: 'evryace/ace-2-values:14'
  return containerTemplate(name: 'ace', image: image, command: 'cat', ttyEnabled: true)
}
