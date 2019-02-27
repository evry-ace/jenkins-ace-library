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
