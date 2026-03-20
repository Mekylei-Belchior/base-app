pipeline {
    agent any
    
    environment {
        // Variáveis úteis
        IMAGE_TAG = "build-${BUILD_NUMBER}-${GIT_COMMIT ?: 'local'}"
        IMAGE_NAME = "app-local:${IMAGE_TAG}"
        K3S_KUBECTL = 'sudo /usr/local/bin/k3s kubectl'
    }
    
    options {
        // Mostrar timestamp nos logs
        timestamps()
        // Mantém os últimos 10 builds
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'ls -la'  // Mostra arquivos para debug
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${IMAGE_NAME}")
                    // Opcional: salvar imagem para cache local
                    sh "docker save ${IMAGE_NAME} -o /tmp/app-${BUILD_NUMBER}.tar"
                }
            }
        }
        
        stage('Deploy to k3s') {
            steps {
                script {
                    try {
                        // Backup do deployment atual
                        sh "${K3S_KUBECTL} get deployment app-deployment -o yaml > /tmp/deployment-backup.yaml || true"
                        
                        // Aplica as configurações
                        sh "${K3S_KUBECTL} apply -f deployment.yaml"
                        sh "${K3S_KUBECTL} apply -f service.yaml"
                        sh "${K3S_KUBECTL} apply -f ingress.yaml"
                        
                        // Aguarda rollout
                        sh "${K3S_KUBECTL} rollout status deployment/app-deployment --timeout=2m"
                    } catch (err) {
                        echo "Erro no deploy: ${err}"
                        currentBuild.result = 'FAILURE'
                        error("Falha no deploy")
                    }
                }
            }
        }
        
        stage('Verify') {
            steps {
                script {
                    // Lista os pods
                    sh "${K3S_KUBECTL} get pods"
                    
                    // Testa se app responde (via port-forward)
                    sh '''
                        POD=$(${K3S_KUBECTL} get pod -l app=base-app -o jsonpath="{.items[0].metadata.name}")
                        ${K3S_KUBECTL} port-forward $POD 3001:3000 &
                        PF_PID=$!
                        sleep 3
                        curl -s -f http://localhost:3001 || exit 1
                        kill $PF_PID
                    '''
                }
            }
        }
    }
    
    post {
        always {
            // Limpeza
            sh 'docker image prune -f || true'
            sh 'rm -f /tmp/app-*.tar || true'
            echo "Pipeline finalizado em: ${new Date()}"
        }
        success {
            echo "✅ Build ${BUILD_NUMBER} concluído com sucesso!"
            // Opcional: notificar via slack/email
        }
        failure {
            echo "❌ Build ${BUILD_NUMBER} falhou!"
            
            // Rollback automático
            script {
                try {
                    sh "${K3S_KUBECTL} rollout undo deployment/app-deployment"
                    sh "${K3S_KUBECTL} rollout status deployment/app-deployment"
                    echo "Rollback executado com sucesso"
                } catch (e) {
                    echo "Erro no rollback: ${e}"
                }
            }
        }
    }
}
