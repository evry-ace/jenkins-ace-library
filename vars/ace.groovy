#!/usr/bin/env groovy

import no.ace.Slack
import no.ace.Docker

def call(global, Map opts = [:], body) {
  def buildAgent = opts.buildAgent ?: 'jenkins-docker-3'
  def dockerSet = opts.containsKey('dockerSet') ? opts.dockerSet : true
  def dockerNameOnly = opts.dockerNameOnly ?: false
  def aceInit = opts.containsKey('aceInit') ? opts.aceInit : true
  def aceFile = opts.aceFile ?: 'ace.yaml'

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

          // Wrap aceDeploy with global and local config
          body.deploy = { dImage, dEnv, dOpts = [:] ->
            dOpts = dOpts + [
              dockerImage: dImage,
              slack: body.slack,
            ]
            aceDeploy(body.global, body.ace, dEnv, dOpts)
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
