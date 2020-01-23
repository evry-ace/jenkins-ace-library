void call(Map opts = [:]) {
  String registry = opts.registry

  String name = opts.name
  String tag = opts.tag

  String imageName = "${registry}/${name}:${tag}"

  String context = opts.context ?: '.'
  String dockerFile = opts.dockerFile ?: 'Dockerfile'

  String cache = opts.cache ? 'true' : 'false'

  println "[ace] Building container with Kaniko - ${imageName}"

  List kanikoOpts = [
    '/kaniko/executor',
    '--cleanup',
    "--context=`pwd`/${context}",
    "--dockerfile=`pwd`/${dockerFile}",
    "--destination=${imageName}",
    "--cache=${cache}",
  ]

  String cmd = kanikoOpts.join(' ').trim()
  println cmd
  container(name: 'kaniko', shell: '/busybox/sh') {
    sh """
    #!/busybox/sh

    export PATH=/busybox:/kaniko:$PATH

    mkdir -p /kaniko/.docker
    cp /.pullsecret/.dockerconfigjson /kaniko/.docker/config.json

    ${cmd}
    """
  }
}
