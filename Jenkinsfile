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
    def groovyImage = 'groovy'
    def groovyVersion = 'alpine'
    def groovyOpts = "--entrypoint=''"

    docker.image("${groovyImage}:${groovyVersion}").inside(groovyOpts) {
      sh 'groovy -classpath src/:vars/:test/ test/AllTestsRunner.groovy'
    }
  }
}
