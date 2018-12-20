#!/usr/bin/env groovy

def call(name, path = '.', opts = [:]) {
  return docker.build(name, "--pull ${path}")
}
