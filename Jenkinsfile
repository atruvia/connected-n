pipeline {
    agent {
        dockerfile { dir 'buildenv' }
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
    }

    stages {
        stage('Run Tests') {
            sh 'mvn verify'
        }
    }
}
