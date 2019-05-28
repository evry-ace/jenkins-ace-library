#!/usr/bin/env groovy
package no.ace

// https://jenkins.io/doc/pipeline/steps/slack/

class Slack implements Serializable {
  Object script
  String channel
  String alerts

  String commitAuthor() {
    String name

    try {
      name = script.sh script: 'git show -s --pretty=%an', returnStdout: true
    } catch (e) {
      name = 'Unknown'
    }

    return name
  }

  String formatMessage(
    String buildStatus = 'STARTED',
    String buildSubject = ''
  ) {
    String buildUrl = script.env.BUILD_URL
    String subject
    String message

    if (buildSubject == '') {
      String jobName = script.env.JOB_NAME
      String buildNum = script.env.BUILD_NUMBER

      String commitAuthor = commitAuthor()
      subject = "${buildStatus}: Job ${jobName} build #${buildNum} by ${commitAuthor}"
    } else {
      subject = "${buildStatus}: ${subject}"
    }

    if (buildStatus == 'PENDING INPUT') {
      message = "${subject} <${buildUrl}input/|${buildUrl}input>"
    } else {
      message = "${subject} <${buildUrl}|${buildUrl}>"
    }

    return message
  }

  Slack(Object script, String channel, String alerts = null) {
    this.script = script
    this.channel = channel
    this.alerts = alerts
  }

  Slack customMessage(String buildStatus, String message) {
    script.slackSend(
      color: 'warning',
      channel: channel,
      notify: false,
      message: formatMessage(buildStatus, message)
    )

    return this
  }

  Slack notifyStarted() {
    script.slackSend(
      color: 'warning',
      channel: channel,
      notify: false,
      message: formatMessage('STARTED')
    )

    return this
  }

  Slack notifyInput(String message = '') {
    script.slackSend(
      color: 'warning',
      channel: channel,
      notify: true,
      message: formatMessage('PENDING INPUT', message)
    )

    return this
  }

  Slack notifyDeploy(String env) {
    script.slackSend(
      color: 'good',
      channel: channel,
      notify: false,
      message: formatMessage("DEPLOYED TO ${env}")
    )

    return this
  }

  Slack notifySuccessful() {
    script.slackSend(
      color: 'good',
      channel: channel,
      notify: false,
      message: formatMessage('SUCCESSFUL')
    )

    return this
  }

  Slack notifyFailed() {
    script.slackSend(
      color: 'danger',
      channel: alerts ?: channel,
      notify: true,
      message: formatMessage('FAILED')
    )

    return this
  }

  Slack notifyAborted() {
    script.slackSend(
      color: 'danger',
      channel: channel,
      notify: false,
      message: formatMessage('ABORTED')
    )

    return this
  }
}
