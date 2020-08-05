#!/usr/bin/env groovy
package no.ace

class Teams implements Serializable {
  Object script
  String notificationsWebhookUrl
  String alertsWebhookUrl

  Teams(Object script, String notificationsWebhookUrl, String alertsWebhookUrl = null) {
    this.script = script
    this.notificationsWebhookUrl = notificationsWebhookUrl
    this.alertsWebhookUrl = alertsWebhookUrl
  }

  Teams notifyStarted() {
    script.office365ConnectorSend(
      message: 'Build was started',
      status: 'STARTED',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifyInput(String message = '') {
    script.office365ConnectorSend(
      message: "Build is waiting for your input (${message})",
      status: 'PENDING INPUT',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifyDeploy(String env) {
    script.office365ConnectorSend(
      message: "We have deployed to ${env}",
      status: "DEPLOYED_TO(${env})",
      color: 'Green',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifySuccessful() {
    script.office365ConnectorSend(
      message: 'Build was successful',
      status: 'SUCCESSFUL',
      color: 'Green',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }

  Teams notifyFailed() {
    script.office365ConnectorSend(
      message: 'Build tripped and failed',
      status: 'FAILED',
      color: 'Red',
      webhookUrl: notificationsWebhookUrl
    )
    return this
  }

  Teams notifyAborted() {
    script.office365ConnectorSend(
      message: 'Build was aborted.',
      status: 'ABORTED',
      color: 'Red',
      webhookUrl: notificationsWebhookUrl
    )

    return this
  }
}
