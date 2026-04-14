#!/bin/bash
set -euo pipefail

echo "🧪 Testando pipeline localmente..."

# -----------------------------
# CONFIG
# -----------------------------
ENVIRONMENT=${ENVIRONMENT:-dev}
BUILD_NUMBER=${BUILD_NUMBER:-$(git rev-parse --short HEAD)}
REGISTRY=${REGISTRY:-192.168.0.106:5000}
IMAGE=${IMAGE:-app}
TAG="${BUILD_NUMBER}-${ENVIRONMENT}"
NAMESPACE="base-app-${ENVIRONMENT}"

echo "🌍 Ambiente: $ENVIRONMENT"
echo "🏷️ Tag: $TAG"

# -----------------------------
# PRE-CHECKS
# -----------------------------
command -v docker    >/dev/null || { echo "❌ docker não encontrado"; exit 1; }
command -v kubectl   >/dev/null || { echo "❌ kubectl não encontrado"; exit 1; }
command -v java      >/dev/null || { echo "❌ java não encontrado (Java 21 necessário)"; exit 1; }
command -v kustomize >/dev/null || { echo "❌ kustomize não encontrado. Instale: https://kubectl.docs.kubernetes.io/installation/kustomize/"; exit 1; }

# Garante acesso ao kubeconfig do k3s sem precisar de sudo
K3S_KUBECONFIG="/etc/rancher/k3s/k3s.yaml"
if [ -r "$K3S_KUBECONFIG" ]; then
    export KUBECONFIG="$K3S_KUBECONFIG"
elif [ -f "$HOME/.kube/config" ]; then
    export KUBECONFIG="$HOME/.kube/config"
else
    echo "❌ kubeconfig não encontrado. Execute:"
    echo "   sudo chmod 644 /etc/rancher/k3s/k3s.yaml"
    echo "   ou: mkdir -p ~/.kube && sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config && sudo chown \$USER ~/.kube/config"
    exit 1
fi

echo "🔑 Usando kubeconfig: $KUBECONFIG"

# -----------------------------
# TESTS
# -----------------------------
echo "🧪 Executando testes..."
./gradlew test --no-daemon --quiet || { echo "❌ Testes falharam"; exit 1; }

# -----------------------------
# BUILD JAR
# -----------------------------
# bootJar reutiliza o bytecode compilado por :test (build incremental do Gradle).
# Sem duplicação de compilação — mesmo artefato que vai para o Docker.
echo "🔨 Build do JAR..."
./gradlew bootJar --no-daemon --quiet || { echo "❌ Build do JAR falhou"; exit 1; }

# -----------------------------
# BUILD
# -----------------------------
# Dockerfile agora só copia o JAR pré-compilado — sem Gradle dentro do Docker.
echo "📦 Build da imagem..."
docker build -t "$IMAGE:$TAG" .

# -----------------------------
# TAG
# -----------------------------
echo "🏷️ Tag da imagem..."
docker tag "$IMAGE:$TAG" "$REGISTRY/$IMAGE:$TAG"

# -----------------------------
# PUSH
# -----------------------------
echo "📤 Push para registry..."
docker push "$REGISTRY/$IMAGE:$TAG"

# -------------------------------------------------------
# DEPLOY — kustomize edit set image (sem sed)
# -------------------------------------------------------
# kustomize edit modifica somente o campo images[], de forma idempotente.
# Trabalhamos numa cópia temporária para não alterar arquivos do repositório.
echo "🧱 Preparando overlay temporário..."

TMP_DIR=$(mktemp -d)
cp -r k8s "$TMP_DIR/"
cd "$TMP_DIR/k8s/overlays/$ENVIRONMENT"

echo "🔧 Atualizando imagem no overlay..."
kustomize edit set image app="$REGISTRY/$IMAGE:$TAG"

echo "🚀 Deploy no k3s..."
kubectl apply -k .

cd - > /dev/null  # volta ao diretório original do projeto

# Aguarda rollout
echo "⏳ Aguardando rollout..."
kubectl rollout status deployment/app-${ENVIRONMENT} --namespace="$NAMESPACE"

rm -rf "$TMP_DIR"

# -----------------------------
# TESTE
# -----------------------------
echo "🌐 Testando aplicação..."

kubectl port-forward service/app-${ENVIRONMENT} 18080:80 --namespace="$NAMESPACE" >/dev/null 2>&1 &
PF_PID=$!
sleep 5

# /actuator/health reflete o estado real do Spring Boot (readiness/liveness).
# /hello é endpoint de negócio — evitar para health checks de infraestrutura.
if curl -fs http://localhost:18080/actuator/health | grep -q '"status":"UP"'; then
    echo "✅ Sucesso!"
else
    echo "❌ Falha!"
    kill $PF_PID 2>/dev/null || true
    exit 1
fi

kill $PF_PID 2>/dev/null || true

# -----------------------------
# LOGS
# -----------------------------
echo "📊 Logs do pod:"
POD=$(kubectl get pod -l app=app --namespace="$NAMESPACE" -o name | head -1 || true)

if [ -n "$POD" ]; then
    kubectl logs "$POD" --namespace="$NAMESPACE"
else
    echo "Nenhum pod encontrado!"
fi

# -----------------------------
# STATUS FINAL
# -----------------------------
echo "📌 Status dos pods:"
kubectl get pods

# -----------------------------
# CLEANUP
# -----------------------------
rm -rf "$TMP_DIR"
rm -f /tmp/rendered.yaml

echo "🎉 Pipeline local finalizada com sucesso!"
