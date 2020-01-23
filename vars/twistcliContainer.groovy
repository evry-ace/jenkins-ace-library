Object call(Map opts = [:]) {
  String image = opts.image ?: 'evryace/twistcli:19.11.506'

  return containerTemplate(
    name: 'twistcli',
    image: image,
    command: 'cat',
    ttyEnabled: true)
}
