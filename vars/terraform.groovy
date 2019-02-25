import no.ace.Terraform

Object call(String path, Map options = [:], Object body) {
  Terraform tf = new Terraform(path, options)
  String args = tf.makeDockerArgs()

  String outPath = tf.planPath(name)

  sh "mkdir -p ${tf.planBasePath()}"

  body.get = { ->
    sh """
    cd ${tf.path}
    terraform get
    """
  }

  body.init = { ->
    sh """
    cd ${tf.path}
    terraform init -no-color ${tf.makeTfArgs()}
    """
  }

  body.plan = { ->
    sh """
    cd ${tf.path}
    terraform plan -no-color -out=${outPath} ${tf.varFilesTf()}
    """
  }

  body.apply = { ->
    sh """
    cd ${tf.path}
    terraform apply -no-color -auto-approve ${tf.varFilesTf()} ${tf.applyPath(name)}
    """
  }

  body.selectOrCreateWorkspace = { ->
    sh """
    cd ${tf.path}

    [ -z `terraform workspace list | grep -w ${workspaceName}` ] && {
      terraform workspace new ${workspaceName}
    } || {
      terraform workspace select ${workspaceName}
    }
    """
  }

  docker.image(tf.tfImage).inside(args) {
    body()
  }
}
