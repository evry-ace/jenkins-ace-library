#!/usr/bin/env groovy

package no.ace

// https://jenkins.io/doc/pipeline/steps/slack/

class Slack implements Serializable {
  def script
  def channel
  def alerts

  Slack(script, channel, alerts = null) {
    this.script = script
    this.channel = channel
    this.alerts = alerts
  }

  def getCommitAuthor() {
    def name = 'Unknown'

    try {
      name = script.sh script: "git show -s --pretty=%an", returnStdout: true
    } catch (e) { }

    return name
  }

  def formatMessage(String buildStatus = 'STARTED', String subject = '') {
    def buildUrl = script.env.BUILD_URL

    if (subject == '') {
      def jobName = script.env.JOB_NAME
      def buildNum = script.env.BUILD_NUMBER

      def commitAuthor = getCommitAuthor()
      subject = "${buildStatus}: Job ${jobName} build #${buildNum} by ${commitAuthor}"
    } else {
      subject = "${buildStatus}: ${subject}"
    }

    if (buildStatus == 'PENDING INPUT') {
      return "${subject} <${buildUrl}input/|${buildUrl}input>"
    } else {
      return "${subject} <${buildUrl}|${buildUrl}>"
    }
  }

  def customMessage(buildStatus, message) {
    script.slackSend(
      color: 'warning',
      channel: channel,
      notify: false,
      message: formatMessage(buildStatus, message)
    )
  }

  def notifyStarted() {
    script.slackSend(
      color: 'warning',
      channel: channel,
      notify: false,
      message: formatMessage('STARTED')
    )
  }

  def notifyInput(String message = '') {
    script.slackSend(
      color: 'warning',
      channel: channel,
      notify: true,
      message: formatMessage('PENDING INPUT', message)
    )
  }

  def notifyDeploy(env) {
    script.slackSend(
      color: 'good',
      channel: channel,
      notify: false,
      message: formatMessage("DEPLOYED TO ${env}")
    )
  }

  def notifySuccessful() {
    script.slackSend(
      color: 'good',
      channel: channel,
      notify: false,
      message: formatMessage('SUCCESSFUL')
    )
  }

  def notifyFailed() {
    script.slackSend(
      color: 'danger',
      channel: alerts ?: channel,
      notify: true,
      message: formatMessage('FAILED')
    )
  }

  def notifyAborted() {
    script.slackSend(
      color: 'danger',
      channel: channel,
      notify: false,
      message: formatMessage('ABORTED')
    )
  }
}
