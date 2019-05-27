#!/usr/bin/env groovy

import no.ace.NoopNotifier
import no.ace.Slack
import no.ace.Teams
import no.ace.Docker

@SuppressWarnings(['MethodSize', 'CyclomaticComplexity'])
Object setupNotifier(Object body) {
  if (body.ace?.contact?.slack || contact?.slack_notifications) {
    String notifications = contact.slack?.notifications ?: contact.slack_notifications
    String alerts = contact.slack?.alerts ?: contact.slack_alerts ?: notifications

    return new Slack(body, notifications, alerts)
  } else if (contact?.teams) {
    Object teams = contact.teams
    String notifications = teams.notifications ?: 'TeamsNotificationWebhook'
    String alerts = teams.alerts ?: notifications

    List<String> creds = []
    if (!notifications.startsWith('https')) {
      creds.add(string(credentialsId: notifications, variable: 'TEAMS_NOTIFY_URL'))
    }

    if (!alerts.startsWith('https')) {
      creds.add(string(credentialsId: alerts, variable: 'TEAMS_ALERT_URL'))
    }

    withCredentials(creds) {
      String notifyUrl = env.TEAMS_NOTIFY_URL ?: notifications
      String alertUrl = env.TEAMS_ALERT_URL ?: alerts
      return new Teams(script, notifyUrl, alertUrl)
    }
  }

  return new NoopNotifier(script)
}

@SuppressWarnings(['MethodSize', 'CyclomaticComplexity'])
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

          Object contact = body.ace?.contact
          body.chat = setupNotifier(contact)

          // Backwards compability with old slack_notifications definition
          if (contact?.slack_notifications) {
            body.slack = body.chat
            deprecatedWarn 'contact.slack_notifications has been deprectated'
            deprecatedWarn 'use contact.slack.notifications instead!'
          }

          body.chat.notifyStarted()

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
        body.chat.notifyFailed()
        throw err
      } finally {
        step([$class: 'WsCleanup'])
        sleep 10
      }
    }
  }
}
