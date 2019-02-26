#!/usr/bin/env groovy

void call(Map opts = [:], Object body) {
  String workspace = opts.workspace ?: '/home/jenkins/workspace'
  String jobName = env.JOB_NAME.replaceAll(/[^A-Za-z0-9]+/, '-')
  String buildNumber = env.BUILD_NUMBER
  String dir = "${workspace}/${jobName}-${buildNumber}"

  ws(dir: dir) {
    body.env.WORKSPACE = dir
    body()
  }
}
