#!/usr/bin/env groovy

void call(String env, List creds, Object body) {
  List credentials = []

  creds.eachWithIndex { cred, index ->
    credentials.add(string(credentialsId: "${env}_${cred.id}", variable: cred.env))
  }

  withCredentials(credentials) {
    body()
  }
}
