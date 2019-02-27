import no.ace.Terraform

Object call(String path, String env, Map opts = [:], Object body) {
  Boolean init = opts.containsKey('init') ? opts.init : true
  String provider = opts.provider ?: 'azure'
  String planFile = opts.planFile ?: "${env}-plan"
  String dockerImage = 'hashicorp/terraform:light'
  String dockerArgs = ["--entrypoint=''", "-e HELM_HOME=${env.WORKSPACE}"].join(' ')

  // terraform state credentials
  // @TODO move to default azure creds
  List stateCreds = opts.stateCreds ?: [
    [id: 'azure_storage_account_name', env: 'AZURE_STORAGE_ACCOUNT'],
    [id: 'azure_storage_access_key', env: 'AZURE_STORAGE_KEY'],
  ]

  // terraform apply credentials
  // @TODO move to default
  List applyCreds = opts.applyCreds ?: [
    [id: 'azure_subscription_id', env: 'TF_VAR_subscription_id'],
    [id: 'azure_client_id', env: 'TF_VAR_client_id'],
    [id: 'azure_client_secret', env: 'TF_VAR_client_secret'],
    [id: 'azure_tenant_id', env: 'TF_VAR_tenant_id'],
  ]

  // extra terraform credentials
  // @TODO remove default
  List extraCreds = opts.extraCreds ?: [
    [id: 'db_password', env: 'TF_VAR_db_password'],
    [id: 'aks_rbac_client_app_id'],
    [id: 'aks_rbac_server_app_id'],
    [id: 'aks_rbac_server_app_secret'],
    [id: 'azure_storage_account_name'],
    [id: 'azure_storage_access_key', env: 'TF_VAR_storage_account_key'],
  ]

  // helper functions used inside terraform dsl
  body.get = { ->
    sh """
    terraform get
    """
  }

  body.init = { ->
    sh """
    terraform init -no-color ${tf.makeTfArgs()}
    """
  }

  body.plan = { ->
    sh """
    terraform plan -no-color -out=${outPath} ${tf.varFilesTf()}
    """
  }

  body.apply = { ->
    sh """
    terraform apply -no-color -auto-approve ${tf.varFilesTf()} ${tf.applyPath(name)}
    """
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
    dir(tf.path) {
      if (init) {
        envCredentials(env, stateCreds, [prefix: 'TF_VAR_']) {
          get()
          init()
          workspace()
        }
      }

      envCredentials(env, applyCreds + extraCreds) {
        body()
      }
    }
  }
}
