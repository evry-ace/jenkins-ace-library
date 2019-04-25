#!/usr/bin/env groovy

void call(String env, List creds, Map opts, Object body) {
  String prefix = opts.prefix ?: ''
  String credsProvider = opts.credsProvider ?: 'jenkins'

  List credentials = []

  creds.eachWithIndex { cred, index ->
    Boolean prependEnv = cred.prependEnv ?: false
    String id = prependEnv ? "${env}_${cred.id}" : cred.id

    String var = cred.env ? cred.env : "${prefix}${cred.id}"
    credentials.add(string(credentialsId: id, variable: var))
  }

  if (credsProvider == 'azureKeyvault') {
    withAzureKeyvault(credentials) {
      body()
    }
  } else if (credsProvider == 'jenkins') {
    withCredentials(credentials) {
      body()
    }
  }
}
