@Grab('org.yaml:snakeyaml:1.23')
import org.yaml.snakeyaml.Yaml

import org.junit.Test
import no.ace.Slack

import static groovy.test.GroovyAssert.shouldFail

class SlackTest extends GroovyTestCase {
  void testConstructor() {
    Slack slack_empty_last = new Slack(this, '#test')
    Slack slack_null_last = new Slack(this, '#test', null)
    Slack slack_all_args = new Slack(this, '#test', '#alerts')
  }
}
