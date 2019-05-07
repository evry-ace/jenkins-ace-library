import no.evry.Docker
import no.evry.Git

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
  Docker dockerUtil = new Docker(script, opts)
  Git git = new Git()

  parameters << [
    img_fqn: dockerUtil.image(opts.registry != null ? opts.registry : ''),
    img_name: dockerUtil.imageName(),
    img_tag: dockerUtil.buildTag(),

    git_pr_id: git.prId(env),
    git_pr_url: git.prUrl(env),
    git_release: git.releaseBranchVersion(env),
    git_commit_short: gitCommit(),
    git_commit: gitCommitHash(),
  ]

  return parameters
}
