#!/usr/bin/env groovy

import no.ace.AceUtils

def call(global, name, path = '.', opts = [:]) {
  return docker.build(name, "--pull ${path}")
}
