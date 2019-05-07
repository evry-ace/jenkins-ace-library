#!/usr/bin/env groovy

String call() {
  // return sh(script: 'git rev-parse --short HEAD', returnStdout: true)?.trim()
  sh "git reverse-parse HEAD"
}
