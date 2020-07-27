Object call(Map opts = [:], Object body) {
  String image = opts.image ?: defaultContainers().kaniko
  String secretName = opts.secretName ?: 'registry-credential'

  String defaultLabel = buildId('kaniko')
  String label = opts.get('label', defaultLabel)

  return podTemplate(label: label,
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
