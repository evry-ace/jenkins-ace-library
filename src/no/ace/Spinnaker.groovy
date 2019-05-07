#!/usr/bin/env groovy
package no.ace

class Spinnaker implements Serializable {
  static List<String> mapToList(Map map) {
    List<String> list = []

    map.keySet().each { key ->
      list.add("${key}=${map[key]}")
    }

    return list
  }

  static List<String> paramsToProperties(Map params, List<Map> artifacts) {
    List<String> data = mapToList(params)

    artifacts.each { artifact ->
      if (artifact.name.contains('/values/')) {
        String key = artifact.name.split('/').last().replace('.yaml', '')

        data.add("values_${key}=${artifact.reference}")
      }

      if (artifact.name =~ /\S+?\d+\.\d+\.\d+(|\S+)\.tgz/) {
        data.add("chart_pkg=${artifact.reference}")
      }
    }

    return data
  }

  void genS3Artifact(List artifacts, String name, String ref) {
    artifacts.push([
      kind: 's3',
      type: 's3/object',
      name: "s3://${name}",
      reference: "s3://${ref}",
    ])
  }

  List<Map> s3HelmArtifactValues(
    List<Object> valuesFiles,
    String name,
    String version,
    Map opts = [:]
  ) {
    List<String> artifacts = []

    String suffix = opts.suffix ?: ''
    String bucket = suffix ? [name, suffix].join('-') : name

    genS3Artifact(
      artifacts,
      "${bucket}/packages/${name}.tgz",
      "${bucket}/packages/${name}-${version}.tgz")

    valueFiles.each { f ->
      String valueFileName = f.name - '.yaml'

      genS3Artifact(
        artifacts,
        "${bucket}/values/${f.name}",
        "${bucket}/values/${valueFileName}-${version}.yaml"
      )
    }

    return artifacts
  }
}
