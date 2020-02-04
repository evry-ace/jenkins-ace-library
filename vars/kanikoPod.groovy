void kanikoPod(Object body, Map opts = [:]) {
  String image = opts.image ?: defaultContainers().kaniko
  String secretName = opts.secretName ?: 'registry-credential'

  podTemplate(yaml: """
  kind: Pod
  spec:
    containers:
    - name: kaniko
      image: ${image}
      imagePullPolicy: Always
      command:
      - /busybox/cat
      tty: true
      volumeMounts:
        - name: jenkins-docker-cfg
          mountPath: /kaniko/.docker
    volumes:
    - name: jenkins-docker-cfg
      projected:
        sources:
        - secret:
            name: ${secretName}
            items:
              - key: .dockerconfigjson
                path: config.json
  """
  ) {
    body.call()
  }
}
