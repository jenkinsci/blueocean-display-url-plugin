pipeline {
  agent { docker 'maven' }
  stages {
    stage('build') {
      steps {
        sh 'mvn clean install' 
      }
    }
  }
  postBuild {
    always {
      junit 'target/**/*.xml'
    }
  }
}
