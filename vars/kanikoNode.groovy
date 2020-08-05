Object call(Map opts = [:], Object body) {
  String defaultLabel = buildId('kaniko')
  String label = opts.get('label', defaultLabel)

  return kanikoPod(opts) {
    node(label) {
      body()
    }
  }
}
