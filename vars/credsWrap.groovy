// Internal utlity function

@SuppressWarnings(['MethodSize', 'CyclomaticComplexity'])
void call(Map opts, Object body) {
  if (opts.k8sConfig) {

    String credVar = 'KUBECONFIG'
    withCredentials([file(credentialsId: opts.k8sConfigCredId, variable: credVar)]) {
      body()
    }
  } else {
    body()
  }
}
