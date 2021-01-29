pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                echo 'Assembling..'
                sh 'ls -la'
                sh 'touch ******.txt'
            }
            post {
                always {
                    
                    sh 'pwd'
                    sh 'mkdir fDir'
                    
                }
            }
        }
    }
}
