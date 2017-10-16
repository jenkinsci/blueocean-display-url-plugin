pipeline {
    agent { docker 'maven' }
    environment {
        GIT_COMMITTER_EMAIL = '=me@hatescake.com'
        GIT_COMMITTER_NAME    = 'Hates'
        GIT_AUTHOR_NAME = 'Cakes'
        GIT_AUTHOR_EMAIL = 'hates@cake.com'
    }
    stages {
        stage('build') {
            steps {
                sh 'mvn clean install'
            }
        }
    }
    post {
        always {
            junit 'target/**/*.xml'
        }
    }
}