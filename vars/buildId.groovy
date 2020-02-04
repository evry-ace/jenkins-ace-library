#!/usr/bin/groovy

/**
 * Returns the id of the build, which consists of the job name,
 * build number and an optional prefix.
 * @param prefix    The prefix to use, defaults in empty string.
 * @return the buildid
 */
String call(String prefix = '') {
  String job = env.JOB_NAME.replaceAll('/', '-')
  String tmpl = "${prefix}${job}_${env.BUILD_NUMBER}"
  String id = tmpl.replaceAll('_', '-').replaceAll('/', '-').replaceAll(' ', '-')
  println "[ace] Build id - ${id}"
  return id
}
