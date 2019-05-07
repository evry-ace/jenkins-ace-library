import no.ace.Spinnaker

class SpinnakerTest extends GroovyTestCase {
  void testParamsToProperties() {
    Map params = [
      test: 1
    ]

    List<Map> artifacts = [
      [
        name: "foo/values/test.yaml",
        reference: "s3://values/test.yaml"
      ]
    ]

    List<String> props = Spinnaker.paramsToProperties(params, artifacts)

    assertEquals(
      [
        "test=1",
        "values_test=s3://values/test.yaml"
      ],
      props
    )
  }

  void tests3HelmArtifactValues() {
    List<Map> valueFiles = [
      [name: "values/prod.yaml"]
    ]

    List<Map> artifacts = Spinnaker.s3HelmArtifactValues(valueFiles, 'test', '1.0.0')

    print artifacts
  }
}
