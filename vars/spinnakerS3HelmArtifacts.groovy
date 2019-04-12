/*
  Helper to take files like chart and value artifacts into a standard payload

  Assuming the follow structure
  chart/<name>
  values/production.yaml
  values/development.yaml

  We end up with
  [
    [
      kind: "s3",
      type: "s3/object",
      name: "s3://<name>-<suffix>/packages/<chart>.tgz",
      reference: "s3://<name>-<suffix>/packages/<chart>.yaml"
    ],
    [
      kind: "s3",
      type: "s3/object",
      name: "s3://<name>-<suffix>/values/development.yaml",
      reference: "s3://<name>-<suffix>/values/development-<version>.yaml"
    ],
    [
      kind: "s3",
      type: "s3/object",
      name: "s3://<name>-<suffix>/values/qa.yaml",
      reference: "s3://<name>-<suffix>/values/qa-<version>.yaml"
    ]
  ]
*/
void _genS3Artifact(List<Object> artifacts, String name, String ref) {
  artifacts.push([
    kind: "s3",
    type: "s3/object",
    name: "s3://${name}",
    reference: "s3://${ref}"
  ])
}

List<Object> call(String name, String version, Map<string, Object> opts = [:]) {
  List<String> artifacts = []

  String suffix = opts.suffix ?: ''

  String bucket = suffix ? [name, suffix].join("-") : name
  _genS3Artifact(artifacts, "${bucket}/packages/${name}.tgz", "${bucket}/packages/${name}-${version}.tgz")

  List<String> valueFiles = findFiles(glob: 'values/**.yaml')
  valueFiles.each {f ->
    String valueFileName = f.name.minus(".yaml")
    _genS3Artifact(artifacts, "${bucket}/values/${f.name}", "${bucket}/values/${valueFileName}-${version}.yaml")
  }

  return artifacts
}
