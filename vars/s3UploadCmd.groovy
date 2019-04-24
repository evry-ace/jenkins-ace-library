/*
  Utility to upload files to S3
*/

void call(String path, String bucket, String remotePath = '', Map opts = [:]) {
  s3Cmd("mb s3://${bucket}", opts)

  String rPath = remotePath ?: '/'

  List<String> uploadCmd = [
    "upload ${path} s3://${bucket}${rPath}",
  ]

  if (opts.recursive) {
    uploadCmd.push('--recurse')
  }

  s3Cmd(uploadCmd.join(' '), opts)
}
