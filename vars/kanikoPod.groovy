Object call(Map opts = [:], Object body) {
  String image = opts.image ?: defaultContainers().kaniko
  String secretName = opts.secretName ?: 'registry-credential'

  String label = opts.label ?: buildId('kaniko')
  String inheritFrom = opts.inheritFrom ?: 'base'

  podTemplate(label: label,
    inheritFrom: inheritFrom,
    yaml: """
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
    """,
  ) {
    body()
  }
}
