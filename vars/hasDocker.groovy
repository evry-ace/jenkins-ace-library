Boolean call() {
  if (fileExists('/var/run/docker.sock')) {
    return true
  } else if (env.DOCKER_HOST) {
    return true
  }

  return false
}
