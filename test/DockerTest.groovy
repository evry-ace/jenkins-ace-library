import no.ace.Docker

class DockerTest extends GroovyTestCase {
  void testConstructor() {
    new Docker(this)
    new Docker(this, [test: 'test'])
  }
}
