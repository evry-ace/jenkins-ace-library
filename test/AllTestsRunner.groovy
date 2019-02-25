import junit.framework.Test
import junit.framework.TestResult
import junit.textui.TestRunner

@SuppressWarnings(['SystemExit'])
class AllTests {
  static Test suite() {
    GroovyTestSuite allTests = new GroovyTestSuite()

    allTests.addTestSuite(ConfigTest)
    allTests.addTestSuite(SlackTest)
    allTests.addTestSuite(DockerTest)

    return allTests
  }

  static void exit(boolean success) {
    System.exit(success ? 0 : 1)
  }
}

TestResult result = TestRunner.run(AllTests.suite())
AllTests.exit(result.wasSuccessful())
