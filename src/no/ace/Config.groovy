#!/usr/bin/env groovy
package no.ace

class Config {
  static Map parse(Map c, String env = '') {
    Map config = this.clone(c)

    // Default values for Helm
    Map helmDefaultValues = this.helmDefaultValues(config.name, env)

    // Common values for Helm
    Map helmCommonValues = config.helm ?: [:]
    config.remove('helm')

    // Deployment environment specific values for Helm
    config.environments = config.environments ?: [:]
    Map helmDeployValues = config.environments[env] ?: [:]
    config.remove('environments')

    // Merge Helm values
    config.helm = this.merge(helmDefaultValues, helmCommonValues, helmDeployValues)

    // @TODO verify required variables

    return config
  }

  static Map helmDefaultValues(String name, String env) {
    return [
      name: "${name}-${env}",
      repo: 'https://evry-ace.github.io/helm-charts',
      repoName: 'ace',
      values: [:],
    ]
  }

  static Map clone(Map orig) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream()
    ObjectOutputStream oos = new ObjectOutputStream(bos)

    oos.writeObject(orig); oos.flush()

    ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray())
    ObjectInputStream ois = new ObjectInputStream(bin)

    return ois.readObject()
  }

  static Map merge(Map... maps) {
    Map result

    if (maps.length == 0) {
      result = [:]
    } else if (maps.length == 1) {
      result = maps[0]
    } else {
      result = [:]
      maps.each { map ->
        map.each { k, v ->
          result[k] = result[k] instanceof Map ? merge(result[k], v) : v
        }
      }
    }

    return result
  }
}
