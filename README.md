# CI/CD - TEMPLATE

Microserviço template de produção construído com **Java 21 + Spring Boot 3.4** seguindo **Arquitetura Hexagonal**. Projetado para ser um ponto de partida realista para novos serviços em ambientes Kubernetes gerenciados com GitOps.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Build | Gradle 9.4.1 |
| Containers | Docker + eclipse-temurin:21-jre-alpine |
| Orquestração | Kubernetes (k3s) |
| Config K8s | Kustomize v5 |
| CI/CD | Jenkins (container) |
| Scan de segurança | Trivy |
| Cobertura | JaCoCo (mínimo 80% LINE) |
| Métricas | Micrometer + Prometheus |
| Documentação API | SpringDoc OpenAPI 3 |

---

## Arquitetura Hexagonal

```
┌──────────────────────────────────────────────────────────────┐
│                        base-app                              │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐     │
│  │               infrastructure                        │     │
│  │                                                     │     │
│  │  adapter.in.rest          adapter.out               │     │
│  │  ┌──────────────────┐    ┌─────────────────┐        │     │
│  │  │ HelloController  │    │  HelloAdapter   │        │     │
│  │  │ GlobalException  │    │  (provideMsg)   │        │     │
│  │  │    Handler       │    └────────┬────────┘        │     │
│  │  └────────┬─────────┘             │                 │     │
│  │           │                       │                 │     │
│  └───────────┼───────────────────────┼─────────────────┘     │
│              │                       │                       │
│         port.in                port.out                      │
│    ┌──────────────┐        ┌────────────────────┐            │
│    │  HelloUseCase│        │HelloMessageProvider│            │
│    │ (interface)  │        │   (interface)      │            │
│    └──────┬───────┘        └────────┬───────────┘            │
│           │                         │                        │
│  ┌────────┴─────────────────────────┴────────────────┐       │
│  │                   application                     │       │
│  │              ┌──────────────┐                     │       │
│  │              │ HelloService │                     │       │
│  │              └──────────────┘                     │       │
│  └───────────────────────────────────────────────────┘       │
│                                                              │
│  ┌───────────────────────────────────────────────────┐       │
│  │                    domain                         │       │
│  │        ┌────────────────────────┐                 │       │
│  │        │  HelloMessage (record) │                 │       │
│  │        └────────────────────────┘                 │       │
│  └───────────────────────────────────────────────────┘       │
└──────────────────────────────────────────────────────────────┘
```

**Regra de dependência:** as camadas mais internas (domain, application) **nunca** dependem das externas (infrastructure). Apenas infrastructure conhece domain e application.

---

## Pré-requisitos

| Ferramenta | Versão mínima | Verificação |
|---|---|---|
| Java | 21 | `java -version` |
| Docker | 24+ | `docker --version` |
| kubectl | 1.28+ | `kubectl version --client` |
| kustomize | 5+ | `kustomize version` |

---

## Setup local

```bash
# Testa, builda e faz deploy no ambiente dev (k3s local)
ENVIRONMENT=dev ./scripts/local.sh

# Para staging
ENVIRONMENT=staging ./scripts/local.sh
```

O script executa em sequência: testes → build JAR → build imagem Docker → push → `kustomize edit set image` → `kubectl apply -k` → aguarda rollout → health check.

---

## Pipeline CI/CD

```
Checkout
   │
Set Environment  ─── branch main → prod
   │             ─── branch release → staging
   │             ─── outros → dev
   │
Test             ─── ./gradlew test (JUnit 5 + JaCoCo)
   │             ─── artefatos: build/reports/jacoco/
   │
Quality Gate     ─── ./gradlew jacocoTestCoverageVerification (mínimo 80% LINE)
   │
Build            ─── docker build (runtime-only, JAR pré-compilado)
   │             ─── docker tag
   │
Security Scan    ─── Trivy (ghcr.io) via Docker socket, cache em volume 'trivy-cache'
   │             ─── escaneia imagem local antes do push
   │             ─── falha em HIGH ou CRITICAL
   │
Push             ─── docker push → 192.168.0.106:5000
   │             ─── somente após scan aprovado
   │
[Approve Prod]   ─── input manual (apenas branch main)
   │
Deploy           ─── kustomize edit set image → kubectl apply -k
   │             ─── kubectl rollout status --timeout=5m
   │
Health Check     ─── curl /actuator/health | grep UP (retry 5x)

Em caso de falha no Deploy:
   └── kubectl rollout undo deployment/<app>-<env> --namespace=base-app-<env>
```

---

## Kubernetes — Overlays por ambiente

| Recurso | dev | staging | prod |
|---|---|---|---|
| Namespace | `base-app-dev` | `base-app-staging` | `base-app-prod` |
| Réplicas | 1 | 1 | gerenciado pelo HPA |
| HPA | — | — | 2–5 réplicas, CPU 70% |
| PDB | — | — | minAvailable: 1 |
| Ingress host | `dev.app.local` | `stg.app.local` | `prod.app.local` |
| Swagger UI | habilitado | habilitado | **desabilitado** |
| Logs | console colorido | console colorido | **JSON estruturado** |
| Spring Profile | — | — | `prod` |

### Aplicar manualmente

```bash
# Dev
kustomize build k8s/overlays/dev | kubectl apply -f -

# Prod
kustomize build k8s/overlays/prod | kubectl apply -f -
```

---

## Secrets

Secrets **nunca** são armazenados no Git. Crie-os manualmente no cluster antes do primeiro deploy:

```bash
# Dev
ENVIRONMENT=dev ./scripts/create-secrets.sh

# Staging
ENVIRONMENT=staging ./scripts/create-secrets.sh

# Prod
ENVIRONMENT=prod ./scripts/create-secrets.sh
```

Os scripts criam o secret `app-secret` no namespace correto de forma idempotente (`--dry-run=client | kubectl apply`).

---

## Endpoints

### API

| Método | Path | Descrição |
|---|---|---|
| `GET` | `/hello` | Retorna mensagem de saudação com timestamp |

**Resposta de exemplo:**
```json
{
  "message": "Hello from k3s 🚀",
  "timestamp": "2026-04-14T10:00:00Z"
}
```

### Actuator

| Path | Descrição | Ambientes |
|---|---|---|
| `/actuator/health` | Status UP/DOWN | todos |
| `/actuator/health/liveness` | Liveness probe (K8s) | todos |
| `/actuator/health/readiness` | Readiness probe (K8s) | todos |
| `/actuator/info` | Informações do build | todos |
| `/actuator/prometheus` | Métricas no formato Prometheus | todos |

### Documentação OpenAPI

| Path | Descrição | Ambientes |
|---|---|---|
| `/swagger-ui.html` | Swagger UI interativo | dev, staging |
| `/v3/api-docs` | Especificação OpenAPI JSON | dev, staging |

---

## Testes e cobertura

```bash
# Roda todos os testes + gera relatório JaCoCo
./gradlew test

# Verifica cobertura mínima (80% LINE) — gate do pipeline
./gradlew jacocoTestCoverageVerification

# Relatório HTML de cobertura
open build/reports/jacoco/test/html/index.html
```

**Suíte atual:** 6 testes / 3 classes — `HelloServiceTest`, `HelloControllerTest`, `GlobalExceptionHandlerTest`.
