#!/bin/bash
set -euo pipefail

echo "🧪 Testando pipeline localmente..."

# -----------------------------
# CONFIG
# -----------------------------
ENVIRONMENT=${ENVIRONMENT:-dev}
BUILD_NUMBER=${BUILD_NUMBER:-1}
REGISTRY=${REGISTRY:-192.168.0.106:5000}
IMAGE=${IMAGE:-app}
TAG="${BUILD_NUMBER}-${ENVIRONMENT}"

echo "🌍 Ambiente: $ENVIRONMENT"
echo "🏷️ Tag: $TAG"

# -----------------------------
# PRE-CHECKS
# -----------------------------
command -v docker >/dev/null || { echo "❌ docker não encontrado"; exit 1; }
command -v kubectl >/dev/null || { echo "❌ kubectl não encontrado"; exit 1; }

# -----------------------------
# BUILD
# -----------------------------
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

# -----------------------------
# PREPARA OVERLAY TEMPORÁRIO
# -----------------------------
echo "🧱 Preparando manifest imutável..."

TMP_DIR=$(mktemp -d)
cp -r k8s "$TMP_DIR/"

cd "$TMP_DIR/k8s/overlays/$ENVIRONMENT"

# -----------------------------
# BUILD DO MANIFEST (KUSTOMIZE EMBUTIDO)
# -----------------------------
echo "🔧 Gerando manifest com kubectl kustomize..."

kubectl kustomize . > /tmp/rendered.yaml

# 🔥 Substitui imagem no YAML gerado (imutável, sem alterar repo)
sed -i "s|image: .*app.*|image: $REGISTRY/$IMAGE:$TAG|g" /tmp/rendered.yaml

# -----------------------------
# DEPLOY
# -----------------------------
echo "🚀 Deploy no k3s..."
kubectl apply -f /tmp/rendered.yaml

# Aguarda rollout
echo "⏳ Aguardando rollout..."
kubectl rollout status deployment/app-${ENVIRONMENT}

# -----------------------------
# TESTE
# -----------------------------
echo "🌐 Testando aplicação..."

kubectl port-forward service/app-${ENVIRONMENT} 3000:80 >/dev/null 2>&1 &
PF_PID=$!
sleep 3

if curl -fs http://localhost:3000; then
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
POD=$(kubectl get pod -l app=app -o name | head -1 || true)

if [ -n "$POD" ]; then
    kubectl logs "$POD"
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
