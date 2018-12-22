#!groovy
@Library("ace") _

ace([dockerSet: false]) {
  stage('Lint') {
    def groovylintImage = 'abletonag/groovylint'
    def groovylintVersion = 'latest'
    def groovylintOpts = "--entrypoint=''"

    docker.image("${groovylintImage}:${groovylintVersion}").inside(groovylintOpts) {
      script = '''
        python3 /opt/run_codenarc.py \
         -report=console \
         -includes="**/*.groovy"
      '''
    }
  }

  stage('Test') {
    def groovyImage = 'groovy'
    def groovyVersion = 'alpine'
    def groovyOpts = "--entrypoint=''"

    docker.image("${groovyImage}:${groovyVersion}").inside(groovyOpts) {
      sh 'groovy -classpath src/:vars/'
    }
  }
}
