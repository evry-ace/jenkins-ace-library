#!/usr/bin/env groovy

/* Utility helper for s3Cmd when other plugins doesnt work */

@SuppressWarnings(['MethodSize', 'CyclomaticComplexity'])
void call(String s3Command, Map opts = [:]) {
  String secretKey = env.AWS_SECRET_ACCESS_KKEY ?: opts.awsSecretKey
  String accessKey = env.AWS_ACCESS_KEY_ID ?: opts.awsAccessKeyId

  List<String> runArgs = [
    "-e AWS_ACCESS_KEY_ID=${accessKey}",
    "-e AWS_SECRET_ACCESS_KEY=${secretKey}",
    "-v $PWD:/data -w /data",
  ]

  List<String> shellCommand = [
    's3cmd',
  ]

  if (opts.host) {
    shellCommand.push("--host ${opts.host}")
  }

  shellCommand.push(s3Command)

  if (opts.cmdOpts) {
    shellCommand.push(opts.cmdOpts)
  }

  String cmd = shellCommand.join(' ')

  docker.image('gerland/docker-s3cmd').inside(runArgs.join(' ')) {
    sh(script: cmd)
  }
}
