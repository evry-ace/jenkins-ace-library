#!/usr/bin/env groovy

import no.ace.Terraform

Map<String, String> call(Object opts = [:]) {
  Terraform tf = new Terraform('', opts)

  return tf.output()
}
