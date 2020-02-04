Object call(Map opts = [:], Object body) {
  String label = opts.label ?: buildId('node')
  String inheritFrom = opts.inheritFrom ?: 'base'

  podTemplate(
    label: label,
    inheritFrom: inheritFrom,
    containers: [containerTemplate(name: 'node', image: 'node:10', command: 'cat', ttyEnabled: true)]
  ) {
    body()
  }
}
