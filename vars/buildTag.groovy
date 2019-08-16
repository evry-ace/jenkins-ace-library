/**
 * getBuildTag generates a build tag using version, date, and short git hash.
 */
String call(Map opts = [:]) {
  String version = opts.version ?: '0.0.0'
  String commit = gitCommit()

  return "${version}-${commit}"
}
