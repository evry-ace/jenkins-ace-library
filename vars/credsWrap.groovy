// Internal utlity function

@SuppressWarnings(['MethodSize', 'CyclomaticComplexity'])
void call(Object creds, Map opts, Object body) {
  if (opts.k8sConfig) {
    withCredentials([file(credentialsId: credId, variable: credVar)]) {
      body()
    }
  } else {
    body()
  }
}
