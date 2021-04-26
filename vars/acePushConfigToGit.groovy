@SuppressWarnings(['MethodSize'])
void call(Map opts = [:]) {
  generateAceValues(opts)

  target = readYaml file: 'target-data/target.yaml'
  cfg = readYaml file: opts.aceFile ?: 'ace.yaml'
  Map gitops = cfg.gitops ?: [:]
  String gitopsRepo = gitops.repo
  String strategy = gitops.strategy ?: 'path'
  String helmChart = target.chart
  String helmChartVersion = target.version

  if (!gitopsRepo) {
    error('[ace] No gitops repo specified, dying.')
  }

  print "[ace] gitops repo strategy is ${strategy}"


  helmPullChart(helmChart, helmChartVersion, opts)

  withCredentials([usernamePassword(
    credentialsId: 'jenkins-git',
    usernameVariable: 'GIT_USER',
    passwordVariable: 'GIT_TOKEN')]
  ) {
    String origin = gitopsRepo.replace(
      'https://', "https://${GIT_USER}:${GIT_TOKEN}@"
    )

    sh """
    set -e
    set -u

    git config --global user.email jenkins@tietoevry.com
    git config --global user.name "Jenkins the autonomous"

    git clone ${origin} gitops
    cd gitops
    git fetch -a
    """

    if (strategy == 'branch') {
      String pushToBranch = gitops.pushToBranch ?: 'test'

      String branches = sh(
        script: 'cd gitops; git branch -a', returnStdout: true).trim()
      println "[ace] Got branches ${branches}"
      Boolean branchExists = branches.contains("remotes/origin/${pushToBranch}")
      println "[ace] Branch ${pushToBranch} exists."

      gitCheckoutArgs = branchExists ? '' : '-b'
      sh "cd gitops; git checkout ${gitCheckoutArgs} ${pushToBranch}"

      sh """
      cd gitops
      CHANGED=''
      [ ! -d "${target.name}" ] && {
        cp -R ../target-data ${target.name}
        CHANGED=y
      } || {
        [ ! -z "`diff -Naur ${target.name} ../target-data`" ] && {
          CHANGED=y
          rm -rf ${target.name}
          cp -R ../target-data ${target.name}
        }
      }

      if [ ! -z "\$CHANGED" ]; then
        git add .
        git commit -m "Update from build - ${opts.tag}"

        git push origin ${pushToBranch}
      fi
      """
    } else if (strategy == 'path') {
      String pushToBranch = gitops.pushToBranch ?: 'master'

      String firstEnv = opts.firstEnv ?: gitops.firstEnv ?: 'test'
      String targetFolder = "${target.name}/${firstEnv}"

      sh """
      cd gitops

      CHANGED=''
      [ ! -d "${targetFolder}" ] && {
        mkdir -p ${targetFolder}
        cp -R ../target-data/* ${targetFolder}
        CHANGED=y
      } || {
        [ ! -z "`diff -Naur ${targetFolder} ../target-data`" ] && {
          CHANGED=y
          rm -rf ${targetFolder}
          cp -R ../target-data ${targetFolder}
        }
      }

      if [ ! -z "\$CHANGED" ]; then
        git add .
        git commit -m "Update from build - ${opts.tag}"

        git push origin ${pushToBranch}
      fi
      """
    }
  }
}
