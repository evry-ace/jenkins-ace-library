#!/usr/bin/env groovy

@SuppressWarnings(['UnusedMethodParameter'])
Object call(String image, List args = [], Map opts = [:], Object body) {
  Boolean hasDocker = docker && fileExists('/var/run/docker.sock')

  if (hasDocker) {
    docker.image(image).inside(args.join('')) {
      body()
    }
  } else {
    container(image) {
      body()
    }
  }
}
