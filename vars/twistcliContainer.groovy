Object call(Map opts = [:]) {
  String image = opts.image ?: 'evryace/twistcli:1'

  return containerTemplate(
    name: 'twistcli',
    image: image,
    command: 'cat',
    ttyEnabled: true)
}
