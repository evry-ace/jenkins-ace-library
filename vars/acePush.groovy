#!/usr/bin/env groovy

import no.ace.Config

def call(config, env, image, opts = [:]) {
  def dryrun = opts.dryrun ?: false

  def ace = Config.parse(config, env)

  def registry = ace.helm.registry
  def name = ace.helm.image

  println "Pushing to Docker Registry"

  withDockerRegistry([credentialsId: registry, url: "https://${registry}"]) {
    println "image=${image}, imageName=${image.imageName()}, imageId=${image.id}"
    println "registry=${registry}, helmName=${name}"

    if (dryrun) {
      println "Docker Push skipped due to dryrun=true"
    } else {
      sh "docker tag ${name} ${registry}/${name}"
      sh "docker push ${registry}/${name}"
    }
  }
}
