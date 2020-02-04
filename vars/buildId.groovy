#!/usr/bin/groovy

/**
 * Returns the id of the build, which consists of the job name,
 * build number and an optional prefix.
 * @param prefix    The prefix to use, defaults in empty string.
 * @return the buildid
 */
String call(String prefix = '') {
  String tmpl = "${prefix}${env.JOB_NAME}_${env.BUILD_NUMBER}"
  return tmpl.replaceAll('-', '_').replaceAll('/', '_').replaceAll(' ', '_')
}
