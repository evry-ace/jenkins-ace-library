Object call(Map opts = [:], Object body) {
  String label = opts.label ?: buildId('kaniko')

  kanikoPod(opts) {
    node(label) {
      body()
    }
  }
}
