import no.ace.Slack

class SlackTest extends GroovyTestCase {
  void testConstructor() {
    new Slack(this, '#test')
    new Slack(this, '#test', null)
    new Slack(this, '#test', '#alerts')
  }
}
