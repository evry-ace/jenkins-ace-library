#!/usr/bin/env groovy

import no.ace.Terraform

void call(String path, Object opts = [:]) {
  Terraform tf = new Terraform(path, opts)

  String args = tf.makeDockerArgs()

  withCredentials(opts.credentials) {
    docker.image(tf.tfLintImage).inside(args) {
      sh """
      cd ${tf.path}

      tflint --var-file=${tf.varFilesTfLint()}
      """
    }
  }
}
