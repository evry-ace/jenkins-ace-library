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
            opts.registry = opts.registry ?: body.ace.helm.registry

            List<String> namePart = body.ace.helm.image.split(':')

            opts.name = opts.name ?: namePart[0]
            opts.tag = opts.tag ?: namePart[1]

            kanikoBuild(opts)
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

            String tag = body.ace.helm.image.split(':')[1]

            generateAceValues(aOpts)

            target = readYaml file: 'target-data/target.yaml'
            cfg = readYaml file: 'ace.yaml'
            Map gitops = cfg.gitops ?: [:]
            String gitopsRepo = gitops.repo

            if (!gitopsRepo) {
              error('[ace] No gitops repo specified, dying.')
            }

            helmPullChart(target.chart, aOpts)

            withCredentials([usernamePassword(
              credentialsId: 'jenkins-git',
              usernameVariable: 'GIT_USER',
              passwordVariable: 'GIT_TOKEN')]
            ) {
              String origin = gitopsRepo.replace(
                'https://', "https://${GIT_USER}:${GIT_TOKEN}@"
              )

              String pushToBranch = gitops.pushToBranch ?: 'test'

              sh """
              set -e
              set -u

              git config --global user.email jenkins@tietoevry.com
              git config --global user.name "Jenkins the autonomous"

              git clone ${origin} gitops
              cd gitops
              git fetch -a
              """

              String branches = sh(
                script: 'cd gitops; git branch -a', returnStdout: true).trim()
              println "[ace] Got branches ${branches}"

              String branchExists = branches.contains("remotes/origin/${pushToBranch}")
              println "[ace] Branch ${pushToBranch} exists."

              gitCheckoutArgs = branchExists ? '' : '-b'
              sh "cd gitops; git checkout ${gitCheckoutArgs} ${pushToBranch}"

              sh """
              cd gitops
              CHANGED=''
              [ ! -d "${target.name}" ] && {
                cp -R ../target-data ${target.name}
                CHANGED=y
              } || {
                [ ! -z "`diff -Naur ${target.name} ../target-data`" ] && {
                  CHANGED=y
                  rm -rf ${target.name}
                  cp -R ../target-data ${target.name}
                }
              }

              if [ ! -z "\$CHANGED" ]; then
                git add .
                git commit -m "Update from build - ${tag}"

                git push origin test
              fi
              """
            }
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
