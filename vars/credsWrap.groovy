// Internal utlity function
@SuppressWarnings(['MethodSize', 'CyclomaticComplexity'])
void call(Map opts, Object body) {
  if (opts.k8sConfigCredId && !env.KUBECONFIG) {
    println "[ace] Using cred id name ${opts.k8sConfigCredId}"

    String credVar = 'KUBECONFIG'
    withCredentials([file(credentialsId: opts.k8sConfigCredId, variable: credVar)]) {
      body()
    }
  } else {
    body()
  }
}
