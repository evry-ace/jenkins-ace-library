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

List<Object> call(String name, String version, Map<string, Object> opts = [:]) {
  valuesFiles = findFiles(glob: 'values/**.yaml')

  return Spinnaker.s3HelmArtifactValues(valuesFiles, name, version, opts)
}
