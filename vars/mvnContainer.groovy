Object call(Map opts = [:]) {
  String image = opts.img ?: 'maven:3.6.1-jdk-11'

  return containerTemplate(name: 'java', image: image, command: 'cat', ttyEnabled: true)
}
