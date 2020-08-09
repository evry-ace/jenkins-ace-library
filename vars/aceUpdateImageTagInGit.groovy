@SuppressWarnings(['MethodSize'])
void call(Map opts = [:]) {
  Map gitops = opts.gitops ?: [:]
  String gitopsRepo = gitops.repo

  String name = opts.name
  String folderName = opts.folderName ?: name

  String tag = opts.tag
  String env = opts.env ?: 'test'

  if (!gitopsRepo) {
    error('[ace] No gitops repo specified, dying.')
  }


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
    """

    String valuesFile = "gitops/${folderName}/${env}/values.yaml"
    println("[ace] Looking for tag ${tag} in ${valuesFile}")

    Map values = readYaml file: valuesFile
    String currentTag = values[name].image.tag
    if (currentTag != tag) {
      sh """
      sed -i "s/tag:.*/tag: ${tag}/g" ${valuesFile}
      """

      println "[ace] Image tag will be updated from ${currentTag} > ${tag}"

      sh """
      cd gitops/

      git config --global user.email jenkins@tietoevry.com
      git config --global user.name "Jenkins the autonomous"

      git add .
      git commit -m "Update from build - ${tag}"
      git push origin master
      """
    }
  }
}
