/**
 * getCommitHash returns the current git commit hash.
 */
def call(Map opts = [:]) {
	// echo "opts: $opts"
	sh "git rev-parse HEAD"
}
