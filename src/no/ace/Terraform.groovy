#!/usr/bin/env groovy
package no.ace

class Terraform implements Serializable {
  Object opts

  String path
  String name
  List<String> varFiles
  String plansPath

  String tfLintImage
  String tfImage
  List<String> cmdArgs

  Terraform(String path = '', Object opts = [:]) {
    this.path = path ?: '.'

    this.opts = opts
    this.varFiles = opts.varFiles ?: []
    this.plansPath = opts.plansPath = 'plans'

    this.tfLintImage = opts.tfLintImage ?: 'wata727/tflint:latest'
    this.tfImage = opts.tfImage ?: 'hashicorp/terraform:light'

    this.cmdArgs = opts.args ?: []
  }

  String makeDockerArgs(List<String> extraArgs = []) {
    List<String> newArgs = ["--entrypoint=''"] + this.cmdArgs + extraArgs
    return newArgs.join(' ')
  }

  String makeTfArgs() {
    List<String> tfArgsList = this.opts.tfArgs ?: []
    return tfArgsList.join(' ')
  }

  String varFilesTf() {
    List<String> files = []

    this.varFiles.each { fn ->
      files.add("--var-file=${fn}")
    }

    return files.join(' ')
  }

  String varFilesTfLint() {
    return this.varFiles.join(',')
  }

  String planBasePath() {
    return "${this.plansPath}"
  }

  String planPath(String name) {
    return "${this.planBasePath()}/${name}-plan"
  }

  /*
    output - generates terraform show for each plan in the planPath
  */
  Map<String, String> output() {
    List<String> files = findFiles(glob: "${this.plansBasePath()}/**-plan")

    Map<String, String> outputs = [:]
    files.each { f ->
      String out = sh(returnStdout: true, script: "terraform show -no-color ${f.path}")
      outputs[f.path] = out
    }

    return outputs
  }

  @SuppressWarnings(['JavaIoPackageAccess'])
  /*
    Either a apply a plan in the planBasePath/name or just apply the current directory
  */
  String applyPath(String name = '') {
    String planPath = this.planPath(name)
    Object isPlan = new File(planPath)

    return applyItemPath = isPlan ? planPath : '.'
  }
}
