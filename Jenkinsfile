pipeline {
  agent any

  stages {
    stage('Build') {
      steps {
        sh 'docker build -t app-local:v1 .'
      }
    }

    stage('Deploy') {
      steps {
        sh 'sudo k3s kubectl apply -f deployment.yaml'
      }
    }
  }
}
