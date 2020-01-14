#!/usr/bin/env groovy

Boolean hasDocker() {
  if (docker) {
    if (fileExists('/var/run/docker.sock')) {
      return true
    } else if (env.DOCKER_HOST) {
      return true
    }
  }

  return false
}

@SuppressWarnings(['UnusedMethodParameter'])
Object call(String image, List args = [], Map opts = [:], Object body) {
  println "[ace] Called with container ${image}"
  if (hasDocker()) {
    docker.image(image).inside(args.join(' ')) {
      body()
    }
  } else {
    container(image) {
      body()
    }
  }
}
