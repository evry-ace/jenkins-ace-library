#!/usr/bin/env groovy

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
