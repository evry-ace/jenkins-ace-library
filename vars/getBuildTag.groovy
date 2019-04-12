/**
 * getBuildTag generates a build tag using version, date, and short git hash.
 */
def call(Map opts = [:]) {
	"${opts.version}-${new Date().format('yyyyMMddHHmmss')}-${getCommitHash()[0..6]}"
}
