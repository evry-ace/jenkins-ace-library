#!groovy
@Library("ace@container-poc") _

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
    def groovyContainer = 'groovy:alpine'
    def groovyOpts = ["--entrypoint=''"]

    aceContainer(groovyContainer, groovyOpts, [:]) {
      sh 'groovy -classpath src/:vars/:test/ test/AllTestsRunner.groovy'
    }
  }
}
