Object call() {
  return containerTemplate(
    name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true)
}
