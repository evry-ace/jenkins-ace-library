/*
  Utility to upload files to S3
*/

void call(String path, String bucket, String remotePath = '', Map opts = [:]) {
  s3Cmd("mb s3://${bucket}", opts)

  remotePath = remotePath ? remotePath : '/'

  List<String> uploadCmd = [
    "upload ${path} s3://${bucket}${remotePath}"
  ]

  if (opts.recursive) {
    uploadCmd.push("--recurse")
  }

  echo "${uploadCmd}"

  s3Cmd(uploadCmd.join(" "), opts)
}
