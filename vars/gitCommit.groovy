String getCommitHash() {
  return sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
}
