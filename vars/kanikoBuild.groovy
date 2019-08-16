def call(Map opts = [:]) {
  String registry = opts.registry

  String name = opts.name
  String tag = opts.tag ?: 'latest'

  String imageName = "${registry}/${name}:${tag}"

  String context = opts.context ?: '.'
  String dockerFile = opts.dockerFile ?: 'Dockerfile'

  println "[ace] Building container with Kaniko"
  println "[ace] Container ${imageName}"

  container(name: 'kaniko', shell: '/busybox/sh') {
      sh """#!/busybox/sh
      mkdir -p /kaniko/.docker; cp /kaniko/.pullsecret/.dockerconfigjson /kaniko/.docker/config.json
      echo " \n *** Building app *** \n"
      /kaniko/executor -c `pwd`/${context} -f `pwd`/${dockerFile} --cleanup --cache=true --destination=${imageName}
      """
  }
}
