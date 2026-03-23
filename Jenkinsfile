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
                cd k8s/overlays/${ENVIRONMENT}
                kustomize edit set image app=$REGISTRY/$IMAGE:$TAG
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
                kubectl apply -k k8s/overlays/${ENVIRONMENT}
                kubectl rollout status deployment/app-${ENVIRONMENT}
                """
            }
        }

        stage('Health Check') {
            steps {
                sh "curl -f http://app.local || exit 1"
            }
        }
    }
}
