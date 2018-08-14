#!/usr/bin/env groovy

import no.ace.AceUtils

def call(global, image, env, opts = [:]) {
  def dryrun = opts.dryrun ?: false

  def utils = new AceUtils(global)
  def cluster = utils.envCluster(env)
  def registry = cluster.registry

  withDockerRegistry([credentialsId: registry, url: "https://${registry}"]) {
    if (dryrun) {
      println "Docker Push skipped due to dryrun=true"
    } else {
      image.push()
    }
  }
}
