#!/usr/bin/env groovy
package no.ace

class NoopNotifier implements Serializable {
  Object script

  NoopNotifier(Object script) {
    this.script = script
  }

  NoopNotifier notifyStarted() {
    return this
  }

  @SuppressWarnings(['UnusedMethodParameter'])
  NoopNotifier notifyInput(String message = '') {
    return this
  }

  @SuppressWarnings(['UnusedMethodParameter'])
  NoopNotifier notifyDeploy(String env) {
    return this
  }

  NoopNotifier notifySuccessful() {
    return this
  }

  NoopNotifier notifyFailed() {
    return this
  }

  NoopNotifier notifyAborted() {
    return this
  }
}
