#!/usr/bin/env groovy

def call() {
  return sh(script: 'git rev-parse --short HEAD', returnStdout: true)?.trim()
}
