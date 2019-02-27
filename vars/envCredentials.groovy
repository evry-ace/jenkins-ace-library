#!/usr/bin/env groovy

void call(String env, List creds, Map opst, Object body) {
  String prefix = opts.prefix ? : ""
  List credentials = []

  creds.eachWithIndex { cred, index ->
    credentials.add(string(
      credentialsId: "${env}_${cred.id}",
      variable: cred.env ? cred.env : "${prefix}${cred.id}"
    ))
  }

  withCredentials(credentials) {
    body()
  }
}
