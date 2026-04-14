pipeline {
    agent any

    environment {
        REGISTRY = "192.168.0.106:5000"
        IMAGE    = "app"
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
                    def branch = (env.GIT_BRANCH ?: '').replaceFirst('origin/', '')
                    if (branch == 'main') {
                        env.ENVIRONMENT = 'prod'
                        env.APP_HOST    = 'prod.app.local'
                    } else if (branch == 'release') {
                        env.ENVIRONMENT = 'staging'
                        env.APP_HOST    = 'stg.app.local'
                    } else {
                        env.ENVIRONMENT = 'dev'
                        env.APP_HOST    = 'dev.app.local'
                    }
                    env.TAG       = "${BUILD_NUMBER}-${env.ENVIRONMENT}"
                    env.NAMESPACE = "base-app-${env.ENVIRONMENT}"
                }
            }
        }

        // ─── Testes + JAR ─────────────────────────────────────────────
        // test compila as classes e roda a suíte.
        // bootJar reutiliza o bytecode já compilado (build incremental do Gradle).
        // Resultado: um único ciclo de compilação em vez de dois.
        stage('Test') {
            steps {
                sh './gradlew test --no-daemon'
                sh './gradlew bootJar --no-daemon'
            }
            post {
                always {
                    junit 'build/test-results/test/**/*.xml'
                    // Arquiva o relatório HTML do JaCoCo para consulta no Jenkins
                    archiveArtifacts artifacts: 'build/reports/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        // ─── Quality Gate — cobertura mínima 80% ──────────────────────
        // Separado do stage Test para que falha de cobertura seja reportada
        // com contexto próprio, sem misturar com falhas de teste.
        stage('Quality Gate') {
            steps {
                sh './gradlew jacocoTestCoverageVerification --no-daemon'
            }
        }

        // ─── Build & Push — docker build só copia o JAR — sem Gradle dentro do Docker ─
        stage('Build & Push') {
            steps {
                sh """
                docker build -t ${IMAGE}:${TAG} .
                docker tag ${IMAGE}:${TAG} ${REGISTRY}/${IMAGE}:${TAG}
                docker push ${REGISTRY}/${IMAGE}:${TAG}
                """
            }
        }

        // ─── Security Scan ────────────────────────────────────────────
        // Trivy roda via Docker — nenhuma instalação necessária no agente.
        // --insecure: aceita o registry local sem TLS (IP + porta 5000).
        // --exit-code 1: falha o pipeline em vulnerabilidades HIGH ou CRITICAL.
        // trivy-cache montado como volume nomeado: o DB (~90 MB) é baixado
        // apenas na primeira execução e reutilizado nas seguintes.
        // --timeout 15m: evita falha por download lento do DB de vulnerabilidades.
        stage('Security Scan') {
            steps {
                sh """
                docker run --rm --network host \
                  -v trivy-cache:/root/.cache/trivy \
                  ghcr.io/aquasecurity/trivy:latest image \
                  --exit-code 1 \
                  --severity HIGH,CRITICAL \
                  --timeout 15m \
                  --insecure \
                  ${REGISTRY}/${IMAGE}:${TAG}
                """
            }
        }

        stage('Approve Prod') {
            when {
                expression { env.ENVIRONMENT == 'prod' }
            }
            steps {
                input message: "Deploy em produção? (${TAG})"
            }
        }

        // ─── Deploy — kustomize edit set image (semântico, idempotente) ────────
        // Trabalhamos numa cópia temporária para não alterar os arquivos do repo.
        // kustomize montado no host via docker-compose.yml.
        stage('Deploy') {
            steps {
                sh """
                TMP=\$(mktemp -d)
                cp -r k8s "\$TMP/"
                cd "\$TMP/k8s/overlays/${env.ENVIRONMENT}"
                kustomize edit set image ${IMAGE}=${REGISTRY}/${IMAGE}:${TAG}
                kubectl apply -k .
                kubectl rollout status deployment/${IMAGE}-${env.ENVIRONMENT} \\
                  --namespace=${env.NAMESPACE} --timeout=5m
                rm -rf "\$TMP"
                """
            }
        }

        // ─── Health Check — actuator em vez de endpoint de negócio ────
        // /actuator/health reflete o estado real da aplicação (Spring Boot lifecycle).
        stage('Health Check') {
            steps {
                retry(5) {
                    sleep 10
                    sh "curl -sf http://${APP_HOST}/actuator/health | grep 'UP'"
                }
            }
        }
    }

    post {
        failure {
            sh "kubectl rollout undo deployment/${IMAGE}-${env.ENVIRONMENT} --namespace=${env.NAMESPACE} || true"
            echo "Deploy falhou — rollback executado para ${IMAGE}-${env.ENVIRONMENT} em ${env.NAMESPACE}"
        }
        always {
            sh 'docker image prune -f || true'
        }
    }
}
