#!/bin/bash

echo "🧪 Testando pipeline localmente..."

# Define um número de build para teste
BUILD_NUMBER=1
export BUILD_NUMBER

# Build da imagem
echo "📦 Build da imagem..."
docker build -t app:local .

echo "🏷️ Tag da imagem..."
docker tag app:local 192.168.0.106:5000/app:latest

echo "📤 Push para registry..."
docker push 192.168.0.106:5000/app:latest

# Salva e carrega a imagem no k3s
echo "📦 Carregando imagem no k3s..."
docker save app-local:build-${BUILD_NUMBER} -o /tmp/app-image.tar
sudo k3s ctr images import /tmp/app-image.tar
rm /tmp/app-image.tar

# Deploy
echo "🚀 Deploy no k3s..."
sed "s/\${BUILD_NUMBER}/${BUILD_NUMBER}/g" k8s/base/deployment.yaml | sudo k3s kubectl apply -f -
sudo k3s kubectl apply -f k8s/base/service.yaml
sudo k3s kubectl apply -f k8s/base/ingress.yaml

# Aguarda pods ficarem prontos
echo "⏳ Aguardando pods ficarem prontos..."
sudo k3s kubectl wait --for=condition=ready pod -l app=base-app --timeout=60s

# Port-forward para teste
echo "✅ Testando aplicação..."
sudo k3s kubectl port-forward service/app-service 3000:80 &
PF_PID=$!
sleep 3

if curl -f http://localhost:3000; then
    echo "✅ Sucesso!"
else
    echo "❌ Falha!"
fi

# Limpa o port-forward
kill $PF_PID 2>/dev/null

# Logs
echo "📊 Logs do pod:"
POD=$(sudo k3s kubectl get pod -l app=base-app -o name | head -1)
if [ -n "$POD" ]; then
    sudo k3s kubectl logs $POD
else
    echo "Nenhum pod encontrado!"
fi

# Status final
echo "📌 Status dos pods:"
sudo k3s kubectl get pods
