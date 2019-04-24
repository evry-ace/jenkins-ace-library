/**
 * getCommitHash returns the current git commit hash.
 */
String call() {
  sh 'git rev-parse HEAD'
}
