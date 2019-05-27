#!/usr/bin/env groovy

import no.ace.Slack
import no.ace.Teams
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
            body.ace.helm = body.ace.helm ?: [:]
            body.ace.helm.image = new Docker(this).image()
          }

          def contact = body.ace?.contact
          if (contact?.slack ||| contact?.slack_notifications) {
            def notifications = contact.slack?.notifications ?: contact.slack_notifications
            def alerts = contact.slack?.alerts ?: contact.slack_alerts ?: channel

            body.chat = new Slack(this, notifications, alerts)
            body.chat.notifyStarted()

            // Backwards compability with old slack_notifications definition
            if (body.ace.contact.slack_notifications) {
              body.slack = body.chat
              println "[DEPRECTION WARNING] contact.slack_notifications has been deprectated"
              println "[DEPRECTION WARNING] use contact.slack.notifications instead!"
            }
          } else if (contact?.teams) {
            def notifications = contact.teams.notifications ?: 'TeamsNotificationWebhook'
            def alerts = contact.teams.alerts ?: notifications

            body.chat = new Teams(this, notifications, alerts)
            body.chat.notifyStarted()
          }

          // Ace Docker Image Build
          body.dockerBuild = { path = '.', opts = [:] ->
            aPath = path ?: '.'
            //opts = opts ?: [:]
            //opts << [slack: body.slack, debug: debug]

            body.image = aceBuild(body.ace.helm.image, aPath)
          }

          // Ace Docker Image Push
          body.dockerPush = { envName = '', opts = [:] ->
            aOpts = opts ?: [:]
            aOpts << [slack: body.slack, debug: debug]

            acePush(body.ace, envName, body.image, aOpts)
          }

          // Ace Helm Deploy
          body.deploy = { envName, opts = [:] ->
            aOpts = opts ?: [:]
            aOpts << [slack: body.slack, debug: debug]

            aceDeploy(body.ace, envName, aOpts)
          }
        }

        body()
      } catch (err) {
        if (body.hasProperty('slack') && body.slack) {
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
