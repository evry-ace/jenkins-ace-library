@Grab('org.yaml:snakeyaml:1.23')
import org.yaml.snakeyaml.Yaml

import no.ace.Config

class ConfigTest extends GroovyTestCase {
  String fixtures = 'test/fixtures'

  void testMerge() {
    Map m1 = [a: 1, animals: [cat: 'blue']]
    Map m2 = [b: 2, animals: [dog: 'red']]

    assert [:] == Config.merge()
    assert m1 == Config.merge(m1)
    assert [a: 1, b: 2, animals: [cat: 'blue', dog: 'red']] == Config.merge(m1, m2)
  }

  void testClone() {
    Map m1 = [a: 1, animals: [[cat: 'blue']]]
    Map m2 = Config.clone(m1)

    m2.b = 2
    m2.animals.push([dog: 'red'])

    assert m1 != m2
    assert m1 == [a: 1, animals: [[cat: 'blue']]]
  }

  void testHelmDefaultValues() {
    assertEquals(Config.helmDefaultValues('foo', 'bar'), [
      name: 'foo-bar',
      repo: 'https://evry-ace.github.io/helm-charts',
      repoName: 'ace',
      values: [:],
    ])
  }

  void testParseBaseConfig() {
    Yaml parser = new Yaml()
    Map minimal = parser.load(("${fixtures}/config-minimal.yaml" as File).text)
    Map minimalDev = parser.load(("${fixtures}/config-minimal-base.yaml" as File).text)

    assert Config.parse(minimal) == minimalDev
  }

  void testParseUnknownConfig() {
    Yaml parser = new Yaml()
    Map minimal = parser.load(("${fixtures}/config-minimal.yaml" as File).text)
    Map minimalDev = parser.load(("${fixtures}/config-minimal-unknown.yaml" as File).text)

    assert Config.parse(minimal, 'unknown') == minimalDev
  }

  void testParseMinimalConfig() {
    Yaml parser = new Yaml()
    Map minimal = parser.load(("${fixtures}/config-minimal.yaml" as File).text)
    Map minimalDev = parser.load(("${fixtures}/config-minimal-dev.yaml" as File).text)

    assert Config.parse(minimal, 'dev') == minimalDev
  }
}
