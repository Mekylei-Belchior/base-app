pipeline {
  agent any

  environment {
    DOCKER_REGISTRY = 'localhost:5000'
    IMAGE_TAG = "${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"
    IMAGE_NAME = "app-local:${IMAGE_TAG}"
    FULL_IMAGE = "${DOCKER_REGISTRY}/app-local:${IMAGE_TAG}"
    K8S_NAMESPACE = 'default'
  }

  stages {

    stage('Build Image') {
      steps {
        script {
          docker.build("app-local:${IMAGE_TAG}")
          
          // Se tiver um registry local
          // sh "docker tag app-local:${IMAGE_TAG} ${FULL_IMAGE}"
          // sh "docker push ${FULL_IMAGE}"
        }
      }
    }

    stage('Deploy to k3s') {
      steps {
        script {
          // Aplica as configurações com a nova imagem
          sh """
            sed -i 's|image:.*|image: app-local:${IMAGE_TAG}|' deployment.yaml
            sudo k3s kubectl apply -f deployment.yaml
            sudo k3s kubectl apply -f service.yaml
            sudo k3s kubectl apply -f ingress.yaml
          """
        }
      }
    }

    stage('Verify Deployment') {
      steps {
        script {
          // Aguarda o rollout
          sh 'sudo k3s kubectl rollout status deployment/app-deployment --timeout=2m'
          
          // Verifica os pods
          sh 'sudo k3s kubectl get pods'
          
          // Testa a aplicação (precisa do curl no container do Jenkins)
          sh '''
            POD_NAME=$(sudo k3s kubectl get pods -l app=base-app -o jsonpath="{.items[0].metadata.name}")
            sudo k3s kubectl port-forward $POD_NAME 3001:3000 &
            sleep 3
            curl -f http://localhost:3001 || exit 1
            kill %1
          '''
        }
      }
    }
  }

  post {
    success {
      echo "✅ Deploy ${IMAGE_TAG} realizado com sucesso!"
    }
    failure {
      echo "❌ Pipeline falhou! Iniciando rollback..."
      
      // Rollback para versão anterior
      script {
        sh 'sudo k3s kubectl rollout undo deployment/app-deployment'
      }
    }
  }
}
