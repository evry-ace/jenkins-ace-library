Object call(Map opts = [:]) {
  String secretName = opts.secretName ?: 'registry-credentials'
  return secretVolume(mountPath: '/kaniko/.pullsecret/',  secretName: secretName)
}
