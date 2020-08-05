void call(Map opts = [:]) {
  String registry = opts.registry

  String name = opts.name
  String tag = opts.tag

  String imageName = "${registry}/${name}:${tag}"

  String context = opts.context ?: '.'
  String dockerFile = opts.dockerFile ?: 'Dockerfile'

  String cache = opts.cache ? 'true' : 'false'
  String copySecret = opts.copySecret ?: 'true'
  String extraArgs = opts.extraArgs ?: ''

  println "[ace] Building container with Kaniko - ${imageName}, opts - ${opts}"

  List kanikoOpts = [
    '/kaniko/executor',
    '--cleanup',
    "--context=`pwd`/${context}",
    "--dockerfile=`pwd`/${dockerFile}",
    "--destination=${imageName}",
    "--cache=${cache}",
    extraArgs,
  ]

  String cmd = kanikoOpts.join(' ').trim()

  if (copySecret == 'true') {
    container(name: 'kaniko', shell: '/busybox/sh') {
      sh """
      #!/busybox/sh

      export PATH=/busybox:/kaniko:$PATH

      mkdir -p /kaniko/.docker
      cp /.pullsecret/.dockerconfigjson /kaniko/.docker/config.json
      """
    }
  }

  container(name: 'kaniko', shell: '/busybox/sh') {
    sh """
    #!/busybox/sh

    export PATH=/busybox:/kaniko:$PATH

    ${cmd}
    """
  }
}
