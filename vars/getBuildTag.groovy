/**
 * getBuildTag generates a build tag using version, date, and short git hash.
 */
String call(Map opts = [:]) {
  return [
    opts.version, new ZonedDateTime().format('yyyyMMddHHmmss'), commitHash[0..6],
  ].join('-')
}
