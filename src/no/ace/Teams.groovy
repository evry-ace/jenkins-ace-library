#!/usr/bin/env groovy
package no.ace

class Teams extends Notifier {
  Object script
  String notificationsWebhookUrl
  String alertsWebhookUrl

  Teams(
    Object script,
    String notificationsWebhookUrl,
    String alertsWebhookUrl = null
  ) {
    this.script = script
    this.notificationsWebhookUrl = notificationsWebhookUrl
    this.alertsWebhookUrl = alertsWebhookUrl
  }

  Teams notifyStarted() {
    String msg = formatMessage(script, 'STARTED')
    script.office365ConnectorSend(
      message: msg,
      status: 'STARTED',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifyInput(String message = '') {
    String msg = formatMessage(script, 'PENDING INPUT', message)
    script.office365ConnectorSend(
      message: msg,
      status: 'PENDING INPUT',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifyDeploy(String env) {
    String msg = formatMessage(script, "DEPLOYED_TO_${env}")
    script.office365ConnectorSend(
      message: msg,
      status: "DEPLOYED_TO ${env}",
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifySuccessful() {
    String msg = formatMessage(script, 'SUCCESSFUL')
    script.office365ConnectorSend(
      message: msg,
      status: 'SUCCESSFUL',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifyFailed() {
    String msg = formatMessage(script, 'FAILED')
    script.office365ConnectorSend(
      message: msg,
      status: 'FAILED',
      webhookUrl: notificationsWebhookUrl
    )
    return this
  }

  Teams notifyAborted() {
    String msg = formatMessage(script, 'ABORTED')
    script.office365ConnectorSend(
      message: msg,
      status: 'ABORTED',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }
}
