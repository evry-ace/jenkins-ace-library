#!/usr/bin/env groovy

package no.ace

class AceUtils implements Serializable {
  def environments

  AceUtils(global) {
    this.environments = global.environments
  }

  /**
   *
   */
  def envCluster(env) {
    if (!environments.containsKey(env)) {
      throw new IllegalArgumentException ("Invalid cluster for environment '${env}'")
    }

    return this.environments[env]
  }

  /**
   * Get fully quallified name for namespace
   *
   * @param env - app deployment environment
   * @param namespace - app base namespace
   * @param envify - convert to fully quallified name
   *
   * @return String with final namespace name
   */
  def envNamespace(env, namespace, envify = true) {
    return envify != false ? "${namespace}-${env}" : namespace
  }

  def appHostname(app, env) {
    return "${app}.${this.envCluster(env).ingress}"
  }

  /**
    * Construct Helm ingress object for given deployment environment
    *
    * @param app - application name
    * @param env - environment name
    * @param envIngress local environment specific configurations
    * @param defIngress global default ingress configurations
    *
    * @return Map with final ingress configuration
    */
  def helmIngress(app, env, envIngress = [:], defIngress = [:]) {
    def ingress = [
      enabled: true,
      internal: true,
      hosts: []
    ] + (defIngress ?: [:]) + (envIngress ?: [:])

    if (ingress.enabled) {
      if (ingress.internal) {
        ingress.hosts.push(this.appHostname(ingress.internalHost ?: app, env))
      }

      ingress.remove('internal')
      ingress.remove('internalHost')

      return ingress
    } else {
      return [enabled: false]
    }
  }
}
