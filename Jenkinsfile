#!groovy

node() {
    docker.image('blueocean_build_env') {
        withEnv(['GIT_COMMITTER_EMAIL=me@hatescake.com','GIT_COMMITTER_NAME=Hates','GIT_AUTHOR_NAME=Cake','GIT_AUTHOR_EMAIL=hates@cake.com']) {
            stage('Building BlueOcean display url') {
                sh "mvn clean install"
                junit 'target/surefire-reports/TEST-*.xml'
                junit 'target/jest-reports/*.xml'
            }
        }
    }
}
