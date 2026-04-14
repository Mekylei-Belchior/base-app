#!/bin/bash
# =============================================================================
# create-secrets.sh
# Cria os Kubernetes Secrets para cada ambiente FORA do Git.
#
# NUNCA versionar senhas/tokens reais em arquivos do repositório.
# Execute este script manualmente no cluster após o primeiro setup ou rotação.
#
# Uso:
#   ENVIRONMENT=dev ./scripts/create-secrets.sh
#   ENVIRONMENT=staging ./scripts/create-secrets.sh
#   ENVIRONMENT=prod ./scripts/create-secrets.sh
#
# O secret criado é referenciado no deployment via:
#   secretRef:
#     name: app-secret
#     optional: true
# =============================================================================

set -euo pipefail

ENVIRONMENT="${ENVIRONMENT:-dev}"

# ---------------------------------------------------------------------------
# Mapeamento de namespaces por ambiente.
# ---------------------------------------------------------------------------
case "$ENVIRONMENT" in
  dev)
    NAMESPACE="base-app-dev"
    ;;
  staging)
    NAMESPACE="base-app-staging"
    ;;
  prod)
    NAMESPACE="base-app-prod"
    ;;
  *)
    echo "❌ Ambiente inválido: '$ENVIRONMENT'. Use: dev | staging | prod"
    exit 1
    ;;
esac

SECRET_NAME="app-secret"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Criando secret '${SECRET_NAME}'"
echo "  Ambiente  : ${ENVIRONMENT}"
echo "  Namespace : ${NAMESPACE}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# ---------------------------------------------------------------------------
# Solicita os valores sensíveis interativamente para não deixar rastro
# em histórico de shell ou variáveis de ambiente.
# Para uso em CI/CD, substitua por leitura de um vault (ex: HashiCorp Vault,
# AWS Secrets Manager, SOPS) ou passe via stdin com `echo "..." | ...`
# ---------------------------------------------------------------------------
read -r -s -p "DB_PASSWORD (deixe em branco se não usar): " DB_PASSWORD
echo

# ---------------------------------------------------------------------------
# Cria ou atualiza o secret de forma idempotente:
#   --dry-run=client -o yaml | kubectl apply → cria SE não existe, atualiza SE existe
# ---------------------------------------------------------------------------
kubectl create secret generic "${SECRET_NAME}" \
  --namespace="${NAMESPACE}" \
  --from-literal="DB_PASSWORD=${DB_PASSWORD}" \
  --dry-run=client -o yaml | kubectl apply -f -

echo ""
echo "✅ Secret '${SECRET_NAME}' criado/atualizado em namespace '${NAMESPACE}'."
echo ""
echo "⚠️  Próximos passos:"
echo "   1. Confirme: kubectl get secret ${SECRET_NAME} -n ${NAMESPACE}"
echo "   2. Para inspecionar (sem revelar valores): kubectl describe secret ${SECRET_NAME} -n ${NAMESPACE}"
echo "   3. Faça o restart do deployment para recarregar o secret:"
echo "      kubectl rollout restart deployment/app-${ENVIRONMENT} -n ${NAMESPACE}"
