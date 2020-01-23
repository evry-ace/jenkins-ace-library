void call(String imageId, Map opts = [:]) {
  Map containers = opts.containers ?: [:]

  String twistcliContainer = containers.twistcli ?: ''
  List<String> twistcliOpts = opts.twistcliOpts ?: []

  String credsId = opts.credsId ?: 'twistlock-creds'
  String consoleAddress = opts.consoleAddress ?: env.TWISTLOCK_CONSOLE_ADDR

  withCredentials([usernamePassword(
    credentialsId: credsId,
    usernameVariable: 'TWISTLOCK_USER',
    passwordVariable: 'TWISTLOCK_PASSWORD')]
  ) {
    println "[ace] Scanning image ${imageId} using console ${consoleAddress}"
    aceContainerWrapper(twistcliContainer, twistcliOpts, [:]) {
      sh """
      #!/bin/sh -x
      which docker

      mkdir /.docker
      cp /.pullsecret/.dockerconfigjson /.docker/config.json
      export DOCKER_CONFIG=/.docker

      docker pull ${imageId}

      twistcli images scan \
        --address ${consoleAddress} \
        --include-files \
        --include-package-files \
        --include-js-dependencies \
        --details \
        --publish \
        --output-file scan.out \
        ${imageId}
        """
    }
  }
}
