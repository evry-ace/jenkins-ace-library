#!/usr/bin/env groovy
package no.ace

class Terraform {
  static Map creds = [
    azure: [
      state: [
        [id: 'azure_storage_account_name', env: 'AZURE_STORAGE_ACCOUNT'],
        [id: 'azure_storage_access_key', env: 'AZURE_STORAGE_KEY'],
      ],
      apply: [
        [id: 'azure_subscription_id', env: 'TF_VAR_subscription_id'],
        [id: 'azure_client_id', env: 'TF_VAR_client_id'],
        [id: 'azure_client_secret', env: 'TF_VAR_client_secret'],
        [id: 'azure_tenant_id', env: 'TF_VAR_tenant_id'],
      ]
    ]
  ]

  static List applyCreds(String provider) {
    return this.creds[provider].apply
  }

  static List stateCreds(String provider) {
    return this.creds[provider].state
  }

  static String varFiles(String dir, String varfile, List extraVarfiles = []) {
    List rawfiles = [varfile] + extraVarfiles
    List outfiles = []

    rawfiles.eachWithIndex { file, i ->
      outfiles.add("-var-file=${dir}/${file}")
    }

    return outfiles.join(' ')
  }
}
