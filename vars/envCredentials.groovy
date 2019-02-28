#!/usr/bin/env groovy

void call(String env, List creds, Map opts, Object body) {
  String prefix = opts.prefix ?: ''
  List credentials = []

  creds.eachWithIndex { cred, index ->
    String id = "${env}_${cred.id}"
    String var = cred.env ? cred.env : "${prefix}${cred.id}"
    credentials.add(string(credentialsId: id, variable: var))
  }

  withCredentials(credentials) {
    body()
  }
}
