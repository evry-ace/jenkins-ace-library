#!groovy
@Library("ace") _

ace([dockerSet: false]) {
  stage('Lint') {
    def groovylintImage = 'abletonag/groovylint'
    def groovylintVersion = 'latest'
    def groovylintOpts = "--entrypoint=''"

    docker.image("${groovylintImage}:${groovylintVersion}").inside(groovylintOpts) {
      sh '''
        python3 /opt/run_codenarc.py -- \
         -report=console \
         -includes="**/*.groovy"
      '''
    }

    publishHTML(target: [
      allowMissing: false,
      alwaysLinkToLastBuild: true,
      keepAll: true,
      reportDir: '',
      reportFiles: 'codenarc-output.html,groovylint-errors.html',
      reportName: 'Groovy Lint Results',
      reportTitles: ''
    ])
  }

  stage('Test') {
    String binPath = "${WORKSPACE}/bin/"

    parallel(
      unit: {
        def groovyContainer = 'groovy:alpine'
        def groovyOpts = ["--entrypoint=''"]

        aceContainer(groovyContainer, groovyOpts, [:]) {
          sh 'groovy -classpath src/:vars/:test/ test/AllTestsRunner.groovy'
        }
      },
      e2e: {
        clusterName = "ace-e2e-${env.BUILD_NUMBER}"
        kubeConfig = "${env.WORKSPACE}/.kube-${clusterName}"

        // Initialize a "kind" cluster, download it's config and then launch a sample app.
        docker.image("alpine").inside() {
          sh """
          mkdir ${binPath}

          wget -O ${binPath}/kind https://github.com/kubernetes-sigs/kind/releases/download/v0.4.0/kind-linux-amd64
          chmod +x ${binPath}/kind

          wget -O docker.tgz https://download.docker.com/linux/static/stable/x86_64/docker-18.06.3-ce.tgz
          tar xvf docker.tgz
          cp docker/docker ${binPath}
          """
        }

        sh """
        export PATH=${binPath}

        kind create cluster --name ${clusterName}
        docker exec -t ${clusterName}-control-plane cat /etc/kubernetes/admin.conf > ${kubeConfig}
        """

        docker.image("evryace/helm-kubectl-terraform:2.14.1__1.13.5__0.12.2").inside() {
          sh """
          export KUBECONFIG=${kubeConfig}
          kubectl get pod

          kubectl apply -f https://raw.githubusercontent.com/jenkinsci/kubernetes-operator/v0.1.1/deploy/crds/jenkins_v1alpha2_jenkins_crd.yaml
          kubectl apply -f https://raw.githubusercontent.com/jenkinsci/kubernetes-operator/v0.1.1/deploy/all-in-one-v1alpha2.yaml

          sed "s#_VERSION_#${env.BRANCH_NAME}#g" jenkins-config.tpl.yaml > jenkins-config.yaml
          kubectl apply -f jenkins-config.yaml
          kubectl apply -f jenkins-backup-pvc.yaml
          kubectl apply -f jenkins.yaml
          """
        }

        sh """
        export PATH=${binPath}
        echo "Cleaning up temporary cluster."
        kind delete cluster --name ${clusterName}
        """

        // docker.image("bsycorp/kind:v1.13.8").wihtRun("--privileged -p 10080:10080 -p 8443:8443") { c ->
        //   docker.inside("alpine") {
        //     sh """
        //     i=0
        //     while True; do
        //       status=`curl -s -o ${env.WORKSPACE}/.kube -I -w "%{http_code}" http://localhost:10080/config`
        //       [ "\$status" == "200" ] && break

        //       sleep 1
        //       [ \$i -ge 10 ] && exit 1
        //       ((i++))
        //     done
        //     """
        //   }


      }
    )
  }
}
