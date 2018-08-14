#!/usr/bin/env groovy

import no.ace.Slack
import no.ace.Docker

def call(global, Map options = [:], body) {
  def buildAgent = options.buildAgent ?: 'jenkins-docker-3'
  def dockerSet = options.containsKey('dockerSet') ? options.dockerSet : true
  def dockerNameOnly = options.dockerNameOnly ?: false
  def aceInit = options.containsKey('aceInit') ? options.aceInit : true
  def aceFile = options.aceFile ?: 'ace.yaml'

  node(buildAgent) {
    buildWorkspace {
      try {
        println "Dedicated to the original ACE, by Alan Turing"

        checkout scm

        if (aceInit) {
          body.ace = readYaml file: aceFile
          body.global = global

          if (dockerSet) {
            body.ace.dockerImageName = new Docker(this).image()
          }

          if (body.ace?.contact?.slack_notifications) {
            def channel = body.ace.contact.slack_notifications
            def alerts = body.ace.contact.slack_alerts ?: channel

            body.slack = new Slack(this, channel, alerts)
            body.slack.notifyStarted()
          }

          // Ace Docker Image Build
          body.build = { path = '.', opts = [:] ->
            path = path ?: '.'
            opts = opts ?: [:]
            opts << [slack: body.slack]
            return aceBuild(body.global, body.ace.dockerImageName, path, opts)
          }

          // Ace Docker Image Push
          body.push = { env, image, dryrun, opts = [:] ->
            opts = opts ?: [:]
            opts << [slack: body.slack, dryrun: dryrun]
            acePush(body.global, image, env, opts)
          }

          // Ace Helm Deploy
          body.deploy = { env, dryrun, opts = [:] ->
            opts = opts ?: [:]
            opts << [slack: body.slack, dryrun: dryrun]
            aceDeploy(body.global, body.ace, env, opts)
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
