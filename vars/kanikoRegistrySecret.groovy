Object call(Map opts = [:]) {
  String secretName = opts.secretName ?: 'registry-credentials'
  return secretVolume(mountPath: '/.pullsecret/',  secretName: secretName)
}
