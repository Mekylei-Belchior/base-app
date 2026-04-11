pipeline {
    agent any

    environment {
        REGISTRY = "192.168.0.106:5000"
        IMAGE = "app"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Set Environment') {
            steps {
                script {
                    if (env.BRANCH_NAME == "main") {
                        env.ENVIRONMENT = "prod"
                    } else if (env.BRANCH_NAME == "release") {
                        env.ENVIRONMENT = "staging"
                    } else {
                        env.ENVIRONMENT = "dev"
                    }

                    env.TAG = "${BUILD_NUMBER}-${ENVIRONMENT}"
                }
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test --no-daemon'
            }
            post {
                always {
                    junit 'build/test-results/test/**/*.xml'
                }
            }
        }

        stage('Build & Push') {
            steps {
                sh """
                docker build -t $IMAGE:$TAG .
                docker tag $IMAGE:$TAG $REGISTRY/$IMAGE:$TAG
                docker push $REGISTRY/$IMAGE:$TAG
                """
            }
        }

        stage('Prepare Manifest') {
            steps {
                sh """
                kubectl kustomize k8s/overlays/${ENVIRONMENT} > /tmp/rendered-${ENVIRONMENT}.yaml
                sed -i "s|image: .*${IMAGE}.*|image: ${REGISTRY}/${IMAGE}:${TAG}|g" /tmp/rendered-${ENVIRONMENT}.yaml
                """
            }
        }

        stage('Approve Prod') {
            when {
                branch 'main'
            }
            steps {
                input message: "Deploy em produção?"
            }
        }

        stage('Deploy') {
            steps {
                sh """
                kubectl apply -f /tmp/rendered-${ENVIRONMENT}.yaml
                kubectl rollout status deployment/app-${ENVIRONMENT} --timeout=5m
                """
            }
        }

        stage('Health Check') {
            steps {
                retry(5) {
                    sleep 10
                    sh "curl -sf http://app.local/hello | grep message"
                }
            }
        }
    }

    post {
        failure {
            sh "kubectl rollout undo deployment/app-${ENVIRONMENT} || true"
            echo "Deploy falhou — rollback executado para app-${ENVIRONMENT}"
        }
        always {
            sh 'docker image prune -f || true'
        }
    }
}
