#!/usr/bin/env groovy

def call(body) {
  def jobName = env.JOB_NAME.replaceAll(/[^A-Za-z0-9]+/, '-')
  def buildNumber = env.BUILD_NUMBER

  ws(dir: "/home/jenkins/workspace/${jobName}-${buildNumber}") {
    body()
  }
}
