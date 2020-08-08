#!/usr/bin/env groovy

import no.ace.NoopNotifier
import no.ace.Slack
import no.ace.Teams
import no.ace.Docker

@SuppressWarnings(['MethodSize', 'CyclomaticComplexity'])
Object setupNotifier(Object body) {
  Object contact = body.ace?.contact

  Object chat
  if (contact?.slack || contact?.slack_notifications) {
    String notifications = contact.slack?.notifications ?: contact.slack_notifications
    String alerts = contact.slack?.alerts ?: contact.slack_alerts ?: notifications

    chat = new Slack(body, notifications, alerts)
  } else if (contact?.teams) {
    Object teams = contact.teams
    String notifications = teams.notifications ?: 'TeamsNotificationWebhook'
    String alerts = teams.alerts ?: notifications

    List<String> creds = []
    if (!notifications.startsWith('https')) {
      println("[ace] Teams using secret: ${alerts} for notifications")
      creds.add(string(credentialsId: notifications, variable: 'TEAMS_NOTIFY_URL'))
    }

    if (!alerts.startsWith('https')) {
      println("[ace] Teams using secret: ${alerts} for alerts")
      creds.add(string(credentialsId: alerts, variable: 'TEAMS_ALERT_URL'))
    }

    withCredentials(creds) {
      String notifyUrl = env.TEAMS_NOTIFY_URL ?: notifications
      String alertUrl = env.TEAMS_ALERT_URL ?: alerts

      println('[ace] Teams webhook url lengths:' +
        "${notifyUrl.length()} ${alertUrl.length()}")
      chat = new Teams(body, notifyUrl, alertUrl)
    }
  } else {
    chat = new NoopNotifier(body)
  }

  return chat
}

@SuppressWarnings(['MethodSize', 'CyclomaticComplexity', 'UnnecessaryObjectReferences'])
void call(Map options = [:], Object body) {
  Boolean debug = options.containsKey('debug') ? options.debug : true
  String buildAgent = options.buildAgent ?: 'jenkins-docker-3'
  Boolean dockerSet = options.containsKey('dockerSet') ? options.dockerSet : true
  Boolean aceInit = options.containsKey('aceInit') ? options.aceInit : true
  String aceFile = options.aceFile ?: 'ace.yaml'

  // Don't care anymore about jobs starting, it's more annoying then good.
  Boolean allowStartupNotification = options.allowStartupNotification ?: false
  Boolean shouldCleanup = options.shouldCleanup ?: true

  String confWorkspace = env.WORKSPACE_PATH ?: options.workspace
  println "[ace] Configured workspace: ${confWorkspace}"

  String workspace
  node(buildAgent) {
    Map containers = [:]
    if (hasDocker()) {
      /*
        This is when we run on a docker enabled worker, then we specify images we want
        vs containers
      */
      containers = defaultContainers()
      workspace = confWorkspace ?: '/home/jenkins/workspace'
    } else {
      // This needs to match the podTemplate you specify
      containers = [
        helm: 'cd',
        kubectl: 'cd',
        terraform: 'cd',
        twistcli: 'twistcli',
        ace: 'ace',
      ]

      /*
        Workspace cleanup is actually only needed when on a jenkins node, when
        in k8s the workspaces are ephemeral.
      */
      shouldCleanup = false
      workspace = confWorkspace ?: '/home/jenkins/agent/workspace'
    }
    println "[ace] Using workspace: ${workspace}"

    if (options.containers) {
      containers << options.containers
    }

    buildWorkspace([workspace: workspace]) {
      try {
        println '[ace] Dedicated to the original ACE, by Alan Turing'

        checkout scm

        if (aceInit) {
          body.ace = readYaml file: aceFile

          if (dockerSet) {
            body.ace.helm = body.ace.helm ?: [:]
            body.ace.helm.image = new Docker(this).image()
            println "[ace] Generated image name: ${body.ace.helm.image}"
          }

          body.chat = setupNotifier(body)
          // Backwards compability with old slack_notifications definition
          if (body.ace?.contact?.slack_notifications) {
            deprecatedWarn 'contact.slack_notifications has been deprectated'
            deprecatedWarn 'use contact.slack.notifications instead!'
          }

          body.slack = body.chat

          if (allowStartupNotification) {
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
            aOpts.containers = aOpts.containers ?: containers
            aOpts << [chat: body.chat, debug: debug]

            acePush(body.ace, envName, body.image, aOpts)
          }

          // Ace Helm Deploy
          body.deploy = { envName, opts = [:] ->
            aOpts = opts ?: [:]
            aOpts.containers = aOpts.containers ?: containers
            aOpts << [chat: body.chat, debug: debug]

            aOpts.image = "${body.ace.helm.registry}/${body.ace.helm.image}"

            generateAceValues(aOpts)

            aceDeploy(body.ace, envName, aOpts)
          }

          body.kaniko = { opts = [:] ->
            aOpts = opts ?: [:]
            aOpts.registry = aOpts.registry ?: body.ace.helm.registry

            List<String> namePart = body.ace.helm.image.split(':')

            aOpts.name = aOpts.name ?: namePart[0]
            aOpts.tag = aOpts.tag ?: namePart[1]

            kanikoBuild(aOpts)
          }

          body.scanWithTwistlock = { opts = [:] ->
            aOpts = opts ?: [:]
            aOpts.containers = aOpts.containers ?: containers

            image = "${body.ace.helm.registry}/${body.ace.helm.image}"

            twistcliScanImage(image, aOpts)
          }

          body.generateValues = { opts = [:] ->
            aOpts = opts ?: [:]
            aOpts.containers = aOpts.containers ?: containers

            aOpts.image = "${body.ace.helm.registry}/${body.ace.helm.image}"

            generateAceValues(aOpts)
          }

          body.pushConfigToGit = { opts = [:] ->
            aOpts = opts ?: [:]
            aOpts.containers = aOpts.containers ?: containers
            aOpts.image = "${body.ace.helm.registry}/${body.ace.helm.image}"
            aOpts.tag = body.ace.helm.image.split(':')[1]
            acePushConfigToGit(aOpts)
          }

          body.updateImageTagInGit = { opts = [:] ->
            aOpts = opts ?: [:]
            aOpts.name = body.ace.name
            aOpts.tag = body.ace.helm.image.split(':')[1]
            aOpts.gitops = body.ace.gitops

            aceUpdateImageTagInGit(aOpts)
          }
        }

        body()
      } catch (err) {
        if (body.hasProperty('chat') && body.chat) {
          body.chat.notifyFailed()
        }
        throw err
      } finally {
        if (shouldCleanup) {
          step([$class: 'WsCleanup'])
          sleep 10
        }
      }
    }
  }
}
