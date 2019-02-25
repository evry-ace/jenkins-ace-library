import junit.framework.Test
import junit.textui.TestRunner

class AllTests {
  static Test suite() {
    GroovyTestSuite allTests = new GroovyTestSuite()

    allTests.addTestSuite(ConfigTest)
    allTests.addTestSuite(SlackTest)
    allTests.addTestSuite(DockerTest)

    return allTests
  }
}

TestRunner.run(AllTests.suite())
