#!/usr/bin/env groovy

def call(message, opts = [:]) {
  def timeout = opts['timeout'] ?: 0
  def timeoutUnit = opts['timeoutUnit'] ?: 'MINUTES'

  def slack = opts.slack ?: null

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
