#!/usr/bin/env groovy

void call(String message, Map opts = [:]) {
  Integer timeout = opts['timeout'] ?: 0
  String timeoutUnit = opts['timeoutUnit'] ?: 'MINUTES'

  Object slack = opts.slack ?: null

  milestone()

  if (slack != null) {
    slack.notifyInput(message)
  }

  if (timeout > 0) {
    timeout(time: timeout, unit: timeoutUnit) {
      input(message)
    }
  } else {
    input(message)
  }

  milestone()
}
