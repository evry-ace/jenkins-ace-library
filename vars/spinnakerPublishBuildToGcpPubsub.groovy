#!/usr/bin/env groovy

/*
  Utility to post data to a pubsub topic

  Requirements:
    opts.secret - a jenkins secret to use in the format of a base 64 encoded
                  holding a GCP SA key
    opts.project - a gcp project
*/
void call(Map opts = [:]) {
  String secret = opts.secret ?: 'gcp-jenkins-pubsub'
  String topic = opts.topic ?: 'build_done'

  List<String> dockerOpts = [
    "-e GCP_KEY=${env.GCP_KEY}",
    '-e GCP_KEY_FILE=/tmp/google-key.json',
    '-u 0:0',
    "-v ${pwd()}:/code",
    '-w /code',
  ]

  withCredentials([
    [
      $class: 'StringBinding',
      credentialsId: secret,
      variable: 'GCP_KEY',
    ],
  ]) {
    docker.image('google/cloud-sdk').inside(dockerOpts.join(' ')) {
      sh """
      echo $GCP_KEY | base64 -d > /tmp/google-key.json
      gcloud auth activate-service-account --key-file=/tmp/google-key.json \
        --project ${opts.project}
      gcloud pubsub topics publish ${topic} --message "`cat build.json`"
      """
    }
  }
}
