#!/usr/bin/env groovy

import no.ace.Config

void call(Map config, String env, Object image, Map opts = [:]) {
  Boolean dryrun = opts.dryrun ?: false

  Map ace = Config.parse(config, env)

  String registry = ace.helm.registry
  String name = ace.helm.image

  println 'Pushing to Docker Registry'

  withDockerRegistry([credentialsId: registry, url: "https://${registry}"]) {
    println "image=${image}, imageName=${image.imageName()}, imageId=${image.id}"
    println "registry=${registry}, helmName=${name}"

    if (dryrun) {
      println 'Docker Push skipped due to dryrun=true'
    } else {
      sh "docker tag ${name} ${registry}/${name}"
      sh "docker push ${registry}/${name}"
    }
  }
}
