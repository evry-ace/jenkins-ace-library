#!/usr/bin/env groovy

import no.ace.Terraform

@SuppressWarnings(['UnnecessaryObjectReferences'])
Object call(String env, Map opts = [:], Object body) {
  Boolean init = opts.containsKey('init') ? opts.init : true
  String provider = opts.provider ?: 'azure'

  String path = opts.path ?: '.'
  String planFile = opts.planFile ?: "${env}-plan"

  String varfilesDir = opts.varfilesDir ?: 'env'
  String varfilesDefault = opts.varfilesDefault ?: "${env}.tfvars"
  List varfilesExtra = opts.varfilesExtra ?: []
  String varfiles = Terraform.varfiles(varfilesDir, varfilesDefault, varfilesExtra)

  String dockerImage = opts.dockerImage ?: 'hashicorp/terraform:light'
  String dockerArgs = ["--entrypoint=''", "-e HELM_HOME=${env.WORKSPACE}"].join(' ')

  // terraform state credentials
  List stateCreds = opts.stateCreds ?: Terraform.stateCreds(provider)

  // terraform apply credentials
  List applyCreds = opts.applyCreds ?: Terraform.applyCreds(provider)

  // extra terraform credentials
  List extraCreds = opts.extraCreds ?: []

  // helper functions used inside terraform dsl
  body.get = { ->
    sh 'terraform get'
  }

  body.init = { ->
    sh "terraform init -no-color ${varfiles}"
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
    sh "terraform apply -no-color -auto-approve ${varfiles} ${planFile}"
  }

  body.workspace = { ->
    sh """
    [ -z `terraform workspace list | grep -w ${workspaceName}` ] && {
      terraform workspace new ${workspaceName}
    } || {
      terraform workspace select ${workspaceName}
    }
    """
  }

  docker.image(dockerImage).inside(dockerArgs) {
    dir(path) {
      if (init) {
        envCredentials(env, stateCreds, [prefix: 'TF_VAR_']) {
          get()
          init()
          workspace()
        }
      }

      envCredentials(env, applyCreds + extraCreds, [prefix: 'TF_VAR_']) {
        body()
      }
    }
  }
}
