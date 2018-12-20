#!/usr/bin/env groovy

import no.ace.Slack
import no.ace.Docker

def call(Map options = [:], body) {
  def debug = options.containsKey('debug') ? options.debug : true
  def workspace = options.workspace ?: '/home/jenkins/workspace'
  def buildAgent = options.buildAgent ?: 'jenkins-docker-3'
  def dockerSet = options.containsKey('dockerSet') ? options.dockerSet : true
  def aceInit = options.containsKey('aceInit') ? options.aceInit : true
  def aceFile = options.aceFile ?: 'ace.yaml'

  node(buildAgent) {
    buildWorkspace([workspace: workspace]) {
      try {
        println "Dedicated to the original ACE, by Alan Turing"

        checkout scm

        if (aceInit) {
          body.ace = readYaml file: aceFile

          if (dockerSet) {
            body.ace.helm.image = new Docker(this).image()
          }

          if (body.ace?.contact?.slack_notifications) {
            def channel = body.ace.contact.slack_notifications
            def alerts = body.ace.contact.slack_alerts ?: channel

            body.slack = new Slack(this, channel, alerts)
            body.slack.notifyStarted()
          }

          // Ace Docker Image Build
          body.dockerBuild = { path = '.', opts = [:] ->
            path = path ?: '.'
            opts = opts ?: [:]
            opts << [slack: body.slack, debug: debug]

            body.image = aceBuild(body.ace.helm.image, path, opts)
          }

          // Ace Docker Image Push
          body.dockerPush = { env = '', opts = [:] ->
            opts = opts ?: [:]
            opts << [slack: body.slack, debug: debug]

            acePush(body.ace, env, body.image, opts)
          }

          // Ace Helm Deploy
          body.deploy = { env, opts = [:] ->
            opts = opts ?: [:]
            opts << [slack: body.slack, debug: debug]

            aceDeploy(body.ace, env, opts)
          }
        }

        body()
      } catch (err) {
        if (body.getBinding().hasVariable('slack')) {
          body.slack.notifyFailed()
        }
        throw err
      } finally {
        step([$class: 'WsCleanup'])
        sleep 10
      }
    }
  }
}
