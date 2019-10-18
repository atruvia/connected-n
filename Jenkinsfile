pipeline {
    agent {
        dockerfile { dir 'buildenv' }
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
    }

    stages {
        stage('Run Tests') {
            steps {
                sh 'mvn verify'
            }
        }
    }
    post {
        failure {
            mail body: "<b>NSpeed CI build failure</b><br>Project: ${env.JOB_NAME} <br>Build Number: ${env.BUILD_NUMBER} <br> URL de build: ${env.BUILD_URL}", charset: 'UTF-8', from: 'NSpeed-Jenkins <noreply@fiduciagad.de', mimeType: 'text/html', replyTo: '', subject: "ERROR CI: Project name -> ${env.JOB_NAME}", to: "peter.fichtner@fiduciagad.de"; 
        }
    }
}
