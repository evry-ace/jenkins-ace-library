#!/usr/bin/env groovy

package no.ace

class Teams implements Notifier {
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
    script.office365ConnectorSend(
      message: formatMessage('STARTED'),
      status: 'STARTED',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifyInput(String message = '') {
    script.office365ConnectorSend(
      message: formatMessage(script, 'PENDING INPUT', message),
      status: 'PENDING INPUT',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifyDeploy(String env) {
    script.office365ConnectorSend(
      message: formatMessage(script, "DEPLOYED_TO_${env}", message),
      status: "DEPLOYED_TO ${env}",
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifySuccessful() {
    script.office365ConnectorSend(
      message: formatMessage(script, 'SUCCESSFUL', message),
      status: 'SUCCESSFUL',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifyFailed() {
    script.office365ConnectorSend(
      message: formatMessage(script, 'FAILED', message),
      status: 'FAILED',
      webhookUrl: notificationsWebhookUrl
    )
    return this
  }

  Teams notifyAborted() {
    script.office365ConnectorSend(
      message: formatMessage(script, 'ABORTED', message),
      status: 'ABORTED',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }
}
