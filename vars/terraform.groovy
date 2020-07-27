#!/usr/bin/env groovy

import no.ace.Terraform

@SuppressWarnings(['MethodSize', 'UnnecessaryObjectReferences'])
void call(String environment, Map opts = [:], Object body) {
  Boolean init = opts.containsKey('init') ? opts.init : true
  String provider = opts.provider ?: 'azure'

  print "[ace] tf provider: ${provider}, init: ${init}"

  String credsProvider = opts.credsProvider ?: 'jenkins'

  String path = opts.path ?: '.'
  String planFile = opts.planFile ?: "${environment}-plan"

  String varfilesDir = opts.varfilesDir ?: 'env'
  String varfilesDefault = opts.varfilesDefault ?: "${environment}.tfvars"
  List varfilesExtra = opts.varfilesExtra ?: []
  String varfiles = Terraform.varFiles(varfilesDir, varfilesDefault, varfilesExtra)

  String dockerImage = opts.dockerImage ?: defaultContainers().terraform
  List<String> dockerArgList = ["--entrypoint=''", "-e HELM_HOME=${env.WORKSPACE}"]

  String workspace = opts.workspace ?: environment
  String credsPrefix = opts.credsPrefix ?: environment

  // terraform state credentials
  List stateCreds = opts.stateCreds ?: Terraform.stateCreds(provider)

  // terraform apply credentials
  List applyCreds = opts.applyCreds ?: Terraform.applyCreds(provider)

  // extra terraform credentials
  List extraCreds = opts.extraCreds ?: []

  String backendConfig = Terraform.backendConfig(stateCreds)

  String terraformRc = opts.terraformRc ?: ''
  if (terraformRc) {
    dockerArgList.push("-e TF_CLI_CONFIG_FILE=${terraformRc}")
  }

  String dockerArgs = dockerArgList.join(' ')

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
    String script = "terraform show -no-color ${planFile}"
    return sh(returnStdout: true, script: script)
  }

  body.output = { String writeToPath, String key = '' ->
    List<String> script = ['terraform', 'output']
    if (key) {
      script.push(key)
    }
    String output = sh(returnStdout: true, script: script.join(' '))
    writeFile file: writeToPath, text: output
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
        envCredentials(
          credsPrefix,
          stateCreds,
          [prefix: 'TF_VAR_', credsProvider: credsProvider,]
        ) {
          body.get()
          body.init()
          body.workspace()
        }
      }

      List creds = applyCreds + stateCreds + extraCreds
      envCredentials(
        credsPrefix,
        creds,
        [prefix: 'TF_VAR_', credsProvider: credsProvider,]
      ) {
        body()
      }
    }
  }
}
