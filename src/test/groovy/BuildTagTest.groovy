import static groovy.test.GroovyAssert.assertTrue

import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class BuildTagTest extends BaseTest {
  Object buildTag

  @Before
  void setUp() {
    super.setUp()
    // load getBuildTag
    buildTag = loadScript('vars/buildTag.groovy')
  }

  @Test
  void testCall() {
    String hash = '9ee0fbdd081d0fa9e9d40dd904309be391e0fb2b'
    String timestamp = ZonedDateTime.now().format('yyyyMMddHHmmss')
    String expected = "1\\.0\\.0\\-${timestamp[0..-3]}[0-5][0-9]\\-${hash[0..6]}"

    // create mock sh step
    helper.registerAllowedMethod('sh', [ String ]) { hash }

    // call getBuildTag and check result
    String result = buildTag(version: '1.0.0')
    assertTrue "result: '$result' not /$expected/", result as String ==~ expected
  }
}
