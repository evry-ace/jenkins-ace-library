#!/usr/bin/env groovy

package no.ace

class Terraform {
  def script
  def opts

  def name
  def varFiles
  def plansPath

  Terraform(opts = [:]) {
    this.opts = opts

    this.name = opts.name ? "default"
    this.path = opts.path ? "."
    this.varFiles = opts.varFiles = []
    this.plansPath = opts.plansPath = "plans"
  }

  def varFilesTf() {
    def files = []

    this.varFiles.each{ fn ->
      files.add("--var-file=${fn}")
    }

    return files.join(" ")
  }

  def varFilesTfLint() {
    return this.varFiles.join(",")
  }

  def planPath() {
    return "${this.plansPath}/${this.name}-plan"
  }

  /*
    plan - generates a plan for each name / path given and puts it in
    the planPath directory.
  */
  def plan() {
    def credentials = opts.credentials ? []

    def varFiles = this.varFilesTf()

    sh "mkdir -p ${this.plansPath}"

    withCredentials(creds) {
      sh """
      cd ${path}
      terraform plan -no-color -out=${this.planPath()} --var-file=${varFiles}
      """
    }
  }

  /*
    output - generates terraform show for each plan in the planPath
  */
  def output() Map<string, string> {
    def files = findFiles(grlob: "${this.plansPath}/**-plan")

    def out = [:]
    files.each{f ->
      def out = sh(returnStdout: true, script: "terraform show -no-color ${f.path}")
      out[f.path] = out
    }

    return out
  }

  def lint() {
    def varFiles = this.varFilesTfLint()

    withCredentials(creds) {
      sh """
      cd ${this.path}
      terraform plan -no-color -out=${this.plansPath}/${this.name}-plan ${varFiles}
      """
    }
  }

  def apply() {
    def varFiles = this.varFilesTfLint()

    withCredentials(creds) {
      sh """
      cd ${this.path}
      terraform apply -no-color -auto-approve ${varFiles} ${this.planPath()}
      """
  }
}
