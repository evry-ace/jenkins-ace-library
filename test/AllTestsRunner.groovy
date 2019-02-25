import junit.framework.Test
import junit.framework.TestResult
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

@SuppressWarnings(['SystemExit'])
TestResult result = TestRunner.run(AllTests.suite())
System.exit(result.wasSuccessful() ? 0 : 1)
