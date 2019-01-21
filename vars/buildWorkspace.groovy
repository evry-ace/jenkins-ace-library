#!/usr/bin/env groovy

def call(Map opts = [:], body) {
  def workspace = opts.workspace ?: '/home/jenkins/workspace'
  def jobName = env.JOB_NAME.replaceAll(/[^A-Za-z0-9]+/, '-')
  def buildNumber = env.BUILD_NUMBER
  def dir = "${workspace}/${jobName}-${buildNumber}"

  ws(dir: dir) {
    body.env.WORKSPACE = dir
    body()
  }
}
