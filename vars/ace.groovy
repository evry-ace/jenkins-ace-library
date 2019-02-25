#!/usr/bin/env groovy

import no.ace.Slack
import no.ace.Docker

void call(Map options = [:], Object body) {
  Boolean debug = options.containsKey('debug') ? options.debug : true
  String workspace = options.workspace ?: '/home/jenkins/workspace'
  String buildAgent = options.buildAgent ?: 'jenkins-docker-3'
  Boolean dockerSet = options.containsKey('dockerSet') ? options.dockerSet : true
  Boolean aceInit = options.containsKey('aceInit') ? options.aceInit : true
  String aceFile = options.aceFile ?: 'ace.yaml'

  node(buildAgent) {
    buildWorkspace([workspace: workspace]) {
      try {
        println 'Dedicated to the original ACE, by Alan Turing'

        checkout scm

        if (aceInit) {
          body.ace = readYaml file: aceFile

          if (dockerSet) {
            body.ace.helm.image = new Docker(this).image()
          }

          if (body.ace?.contact?.slack_notifications) {
            String channel = body.ace.contact.slack_notifications
            String alerts = body.ace.contact.slack_alerts ?: channel

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
          body.dockerPush = { envName = '', opts = [:] ->
            opts = opts ?: [:]
            opts << [slack: body.slack, debug: debug]

            acePush(body.ace, envName, body.image, opts)
          }

          // Ace Helm Deploy
          body.deploy = { envName, opts = [:] ->
            opts = opts ?: [:]
            opts << [slack: body.slack, debug: debug]

            aceDeploy(body.ace, envName, opts)
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
