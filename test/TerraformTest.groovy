import no.ace.Terraform

class TerraformTest extends GroovyTestCase {
  void testConstructor() {
    new Terraform('.', [:])
  }

  void testVarFilesLint() {
    List<String> varFiles = ['1.tfvars', '2.tfvars']
    Terraform tf = new Terraform('.', [varFiles: varFiles])

    String files = tf.varFilesTfLint()
    assert files == '1.tfvars,2.tfvars'
  }

  void testVarFiles() {
    List<String> varFiles = ['1.tfvars', '2.tfvars']
    Terraform tf = new Terraform('.', [varFiles: varFiles])

    String files = tf.varFilesTf()
    assert files == '--var-file=1.tfvars --var-file=2.tfvars'
  }

  void testMakeArgs() {
    Terraform tf = new Terraform('.', [args: ['-e FOO=1']])

    String args = tf.makeDockerArgs()
    assert args == '--entrypoint=\'\' -e FOO=1'
  }

  void testMakeArgsWithExtra() {
    Terraform tf = new Terraform('.', [args: ['-e FOO=1']])

    String args = tf.makeDockerArgs(['-e BAR=1'])
    assert args == '--entrypoint=\'\' -e FOO=1 -e BAR=1'
  }
}
