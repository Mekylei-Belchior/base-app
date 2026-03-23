pipeline {
    agent any

    environment {
        REGISTRY = "192.168.0.106:5000"
        IMAGE = "app"
        TAG = "${BUILD_NUMBER}"
    }

    stages {

        stage('Build Image') {
            steps {
                sh "docker build -t $IMAGE:$TAG ."
            }
        }

        stage('Tag Image') {
            steps {
                sh "docker tag $IMAGE:$TAG $REGISTRY/$IMAGE:$TAG"
                sh "docker tag $IMAGE:$TAG $REGISTRY/$IMAGE:latest"
            }
        }

        stage('Push Image') {
            steps {
                sh "docker push $REGISTRY/$IMAGE:$TAG"
                sh "docker push $REGISTRY/$IMAGE:latest"
            }
        }

        stage('Deploy to k3s') {
            steps {
                sh """
                kubectl set image deployment/app app=$REGISTRY/$IMAGE:$TAG || true
                kubectl apply -f k3s/
                kubectl rollout status deployment/app
                """
            }
        }

        stage('Health Check') {
            steps {
                sh "curl -f http://app.local || exit 1"
            }
        }
    }

    post {
        failure {
            echo "Deploy falhou"
        }
        success {
            echo "Deploy realizado com sucesso 🚀"
        }
    }
}
