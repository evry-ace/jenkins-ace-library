@Grab('org.yaml:snakeyaml:1.23')
import org.yaml.snakeyaml.Yaml

import org.junit.Test
import no.ace.Docker

import static groovy.test.GroovyAssert.shouldFail

class DockerTest extends GroovyTestCase {
  void testConstructor() {
    Docker docker_empty_last = new Docker(this)
    Docker docker_all_args = new Docker(this, [test: 'test'])
  }
}
