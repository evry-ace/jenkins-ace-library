import static groovy.test.GroovyAssert.assertEquals

import org.junit.Before
import org.junit.Test

class CommitHashTest extends BaseTest {
  Object gitCommitHash

  @Before
  void setUp() {
    super.setUp()
    // load getCommitHash
    gitCommitHash = loadScript('vars/gitCommitHash.groovy')
  }

  @Test
  void testCall() {
    String hash = '9ee0fbdd081d0fa9e9d40dd904309be391e0fb2b'

    // create mock sh step
    helper.registerAllowedMethod('sh', [ String ]) { hash }

    // call getCommitHash and check result
    String result = gitCommitHash()
    assertEquals 'result:', hash, result
  }
}
