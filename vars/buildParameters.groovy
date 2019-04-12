import no.evry.Spinnaker

/*
  Function to make parameters about the build

  img_fqn - image fully qualified name
  img_name - Image name
  img_tag - Image tag

  git_pr_id - PR id if any.
  git_pr_url - URL of the PR.
  git_release - Release idenfitier.
  git_commit_short - Shortened part of the git commit id.
  git_commit - Full git commit id.
*/

Map<String, Object> call(Object script, Map parameters = [:], Map opts = [:]) {
  Docker docker = new Docker(script, opts)
  Git git = new Git()

  parameters.img_fqn = docker.image(opts.registry != null ? opts.registry : '')
  parameters.img_name = docker.imageName()
  parameters.img_tag = docker.buildTag()

  parameters.git_pr_id = git.prId(env)
  parameters.git_pr_url = git.prUrl(env)
  parameters.git_release = git.releaseBranchVersion(env)
  parameters.git_commit_short = git.gitShortCommit(script)
  parameters.git_commit = git.gitCommit(script)

  return parameters
}
