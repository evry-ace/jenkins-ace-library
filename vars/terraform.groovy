#!/usr/bin/env groovy

import no.ace.Terraform

@SuppressWarnings(['UnnecessaryObjectReferences'])
Object call(String environment, Map opts = [:], Object body) {
  Boolean init = opts.containsKey('init') ? opts.init : true
  String provider = opts.provider ?: 'azure'

  String path = opts.path ?: '.'
  String planFile = opts.planFile ?: "${environment}-plan"

  String varfilesDir = opts.varfilesDir ?: 'env'
  String varfilesDefault = opts.varfilesDefault ?: "${environment}.tfvars"
  List varfilesExtra = opts.varfilesExtra ?: []
  String varfiles = Terraform.varfiles(varfilesDir, varfilesDefault, varfilesExtra)

  String dockerImage = opts.dockerImage ?: 'hashicorp/terraform:light'
  String dockerArgs = ["--entrypoint=''", "-e HELM_HOME=${env.WORKSPACE}"].join(' ')

  String workspace = opts.workspace ?: environment
  String credsPrefix = opts.credsPrefix ?: environment

  // terraform state credentials
  List stateCreds = opts.stateCreds ?: Terraform.stateCreds(provider)

  // terraform apply credentials
  List applyCreds = opts.applyCreds ?: Terraform.applyCreds(provider)

  // extra terraform credentials
  List extraCreds = opts.extraCreds ?: []

  String backendConfig = Terraform.backendConfig(stateCreds)

  // helper functions used inside terraform dsl
  body.get = { ->
    sh 'terraform get'
  }

  body.init = { ->
    sh "terraform init -no-color ${backendConfig}"
  }

  body.lint = { ->
    sh 'terraform fmt -check=true'
  }

  body.plan = { ->
    sh "terraform plan -no-color -out=${planFile} ${varfiles}"
  }

  body.show = { ->
    String script = "terraform plan -no-color ${planFile}"
    return sh(returnStdout: true, script: script)
  }

  body.apply = { ->
    sh "terraform apply -no-color -auto-approve ${planFile}"
  }

  body.workspace = { ->
    sh """
    [ -z `terraform workspace list | grep -w ${workspace}` ] && {
      terraform workspace new ${workspace}
    } || {
      terraform workspace select ${workspace}
    }
    """
  }

  docker.image(dockerImage).inside(dockerArgs) {
    dir(path) {
      if (init) {
        envCredentials(credsPrefix, stateCreds, [prefix: 'TF_VAR_']) {
          body.get()
          body.init()
          body.workspace()
        }
      }

      List creds = applyCreds + stateCreds + extraCreds
      envCredentials(credsPrefix, creds, [prefix: 'TF_VAR_']) {
        body()
      }
    }
  }
}
