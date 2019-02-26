#!/usr/bin/env groovy

Object call(String name, String path = '.') {
  return docker.build(name, "--pull ${path}")
}
