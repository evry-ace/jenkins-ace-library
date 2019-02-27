import no.ace.Terraform

class TerraformTest extends GroovyTestCase {
  void testAzureApplyCreds() {
    assert Terraform.applyCreds('azure') == [
      [id: 'azure_subscription_id', env: 'TF_VAR_subscription_id'],
      [id: 'azure_client_id', env: 'TF_VAR_client_id'],
      [id: 'azure_client_secret', env: 'TF_VAR_client_secret'],
      [id: 'azure_tenant_id', env: 'TF_VAR_tenant_id'],
    ]
  }

  void testAzureStateCreds() {
    assert Terraform.stateCreds('azure') == [
      [id: 'azure_storage_account_name', env: 'AZURE_STORAGE_ACCOUNT'],
      [id: 'azure_storage_access_key', env: 'AZURE_STORAGE_KEY'],
    ]
  }

  void testCredsEnvify() {
    List credsIn = [
      [id: 'foo_cred'],
      [id: 'foobar_cred', env: 'TF_VAR_foobar_cred'],
      [id: 'bar_cred'],
      [id: 'barfoo_cred', env: 'TF_VAR_barfoo_cred'],
    ]

    List credsOut = [
      [credentialsId: 'test_foo_cred', variable: 'TF_VAR_foo_cred'],
      [credentialsId: 'test_foobar_cred', variable: 'TF_VAR_foobar_cred'],
      [credentialsId: 'test_bar_cred', variable: 'TF_VAR_bar_cred'],
      [credentialsId: 'test_barfoo_cred', variable: 'TF_VAR_barfoo_cred'],
    ]

    assert Terraform.credsEnvify('test', credsIn) == credsOut
  }

  void testVarFiles() {
    assert Terraform.varFiles('../env', 'common.tfvars', []) == [
      '-var-file=../env/common.tfvars',
    ].join(' ')

    assert Terraform.varFiles('env', 'test.tfvars', ['common.tfvars']) == [
      '-var-file=env/test.tfvars',
      '-var-file=env/common.tfvars',
    ].join(' ')
  }
}
