#!/usr/bin/env groovy
package no.ace

// https://jenkins.io/doc/pipeline/steps/slack/

class Slack implements Notifier {
  Object script
  String channel
  String alerts

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
      message: formatMessage(script, buildStatus, message)
    )

    return this
  }

  Slack notifyStarted() {
    script.slackSend(
      color: 'warning',
      channel: channel,
      notify: false,
      message: formatMessage(script, 'STARTED')
    )

    return this
  }

  Slack notifyInput(String message = '') {
    script.slackSend(
      color: 'warning',
      channel: channel,
      notify: true,
      message: formatMessage(script, 'PENDING INPUT', message)
    )

    return this
  }

  Slack notifyDeploy(String env) {
    script.slackSend(
      color: 'good',
      channel: channel,
      notify: false,
      message: formatMessage(script, "DEPLOYED TO ${env}")
    )

    return this
  }

  Slack notifySuccessful() {
    script.slackSend(
      color: 'good',
      channel: channel,
      notify: false,
      message: formatMessage(script, 'SUCCESSFUL')
    )

    return this
  }

  Slack notifyFailed() {
    script.slackSend(
      color: 'danger',
      channel: alerts ?: channel,
      notify: true,
      message: formatMessage(script, 'FAILED')
    )

    return this
  }

  Slack notifyAborted() {
    script.slackSend(
      color: 'danger',
      channel: channel,
      notify: false,
      message: formatMessage(script, 'ABORTED')
    )

    return this
  }
}
