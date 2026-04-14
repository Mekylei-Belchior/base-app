# Architecture — base-app

> For coding standards, implementation guidelines, and AI code-generation rules, see [/docs/ai-context.md](ai-context.md).

---

## Project Overview

`base-app` is a **production-ready Java microservice template** built with Java 21 and Spring Boot 3.4. Its purpose is to be the canonical starting point for new services in the organization, providing a consistent foundation for architecture, security hardening, observability, and CI/CD.

- **Group / Artifact:** `com.baseapp` / `base-app`
- **Entrypoint:** `com.baseapp.BaseAppApplication`
- **Packaging:** fat JAR (`base-app.jar`), containerized as `eclipse-temurin:21-jre-alpine`
- **Runtime:** Kubernetes (k3s), deployed via Kustomize overlays

---

## Technology Stack

| Concern | Technology | Version |
|---------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.4.5 |
| Build tool | Gradle | 9.4.1 |
| Container base image | eclipse-temurin JRE Alpine | 21 |
| Container orchestration | Kubernetes (k3s) | — |
| K8s config management | Kustomize | v5 |
| CI/CD | Jenkins | — |
| Container security scanning | Trivy | — |
| Code coverage | JaCoCo | — |
| Metrics | Micrometer + Prometheus | — |
| Structured logging | Logstash Logback Encoder | 8.1 |
| API documentation | SpringDoc OpenAPI 3 | 2.8.6 |

---

## Architectural Style — Hexagonal Architecture (Ports & Adapters)

The service follows **Hexagonal Architecture**, also known as Ports & Adapters. The goal is to isolate the domain and business logic from external infrastructure concerns (HTTP, databases, message brokers, external APIs).

### Core principle

> The domain and application layers must never depend on infrastructure. Infrastructure depends on the domain.

```
┌──────────────────────────────────────────────────────────────────┐
│                            base-app                              │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                      infrastructure                       │   │
│  │                                                           │   │
│  │  adapter.in.rest                  adapter.out             │   │
│  │  ┌────────────────────────┐    ┌──────────────────┐       │   │
│  │  │   HelloController      │    │   HelloAdapter   │       │   │
│  │  │   GlobalException      │    │  (provideMsg)    │       │   │
│  │  │       Handler          │    └────────┬─────────┘       │   │
│  │  └──────────┬─────────────┘             │                 │   │
│  └─────────────┼─────────────────────────  ┼─────────────────┘   │
│                │                           │                     │
│           port.in (driving)           port.out (driven)          │
│    ┌──────────────────────┐    ┌───────────────────────────┐     │
│    │     HelloUseCase     │    │   HelloMessageProvider    │     │
│    │     (interface)      │    │       (interface)         │     │
│    └──────────┬───────────┘    └────────────┬──────────────┘     │
│               │                             │                    │
│  ┌────────────┴─────────────────────────────┴──────────────────┐ │
│  │                        application                          │ │
│  │                   ┌──────────────────┐                      │ │
│  │                   │   HelloService   │                      │ │
│  │                   └──────────────────┘                      │ │
│  └──────────────────────────────────────────────────────────── ┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │                          domain                            │   │
│  │              ┌────────────────────────────┐                │   │
│  │              │   HelloMessage (record)    │                │   │
│  │              └────────────────────────────┘                │   │
│  └────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

### Request flow

```
HTTP Request
    → HelloController          (infrastructure.adapter.in.rest)
    → HelloUseCase.execute()   (domain.port.in — interface)
    → HelloService             (application.service — implementation)
    → HelloMessageProvider     (domain.port.out — interface)
    → HelloAdapter             (infrastructure.adapter.out — implementation)
         ↩ raw String
    ↩ HelloMessage (domain record built in HelloService)
    → HelloResponse.from(...)  (DTO mapping in controller)
HTTP Response
```

---

## Package Layout

```
com.baseapp
├── BaseAppApplication.java            # Spring Boot entry point
├── domain
│   ├── model                          # Immutable value objects (Java records)
│   │   └── HelloMessage.java
│   └── port
│       ├── in                         # Driving ports — what the app offers
│       │   └── HelloUseCase.java
│       └── out                        # Driven ports — what the app requires
│           └── HelloMessageProvider.java
├── application
│   └── service                        # Use case implementations (orchestration only)
│       └── HelloService.java
└── infrastructure
    ├── adapter
    │   ├── in
    │   │   └── rest                   # REST controllers + response DTOs
    │   │       ├── HelloController.java
    │   │       ├── GlobalExceptionHandler.java
    │   │       └── dto
    │   │           └── HelloResponse.java
    │   └── out                        # Outbound adapters (DB, config, external APIs)
    │       └── HelloAdapter.java
    └── config                         # Spring @Configuration classes
        └── MetricsConfig.java
```

### Layer responsibilities

| Layer | Package | Responsibility |
|-------|---------|---------------|
| **Domain** | `domain.model`, `domain.port.*` | Business invariants. Immutable records. No Spring. |
| **Application** | `application.service` | Orchestrates use cases. Calls outbound ports. Builds domain objects. |
| **Infrastructure — in** | `infrastructure.adapter.in.rest` | Receives HTTP, validates input, maps domain → DTO. |
| **Infrastructure — out** | `infrastructure.adapter.out` | Implements driven ports. Connects to DB, config, external systems. |
| **Infrastructure — config** | `infrastructure.config` | Spring wiring, metrics tags, security config. |

---

## Architectural Decisions

### ADR-1: Hexagonal Architecture

**Decision:** Adopt Ports & Adapters as the sole structural pattern.  
**Rationale:** Enables testing the full business logic with zero Spring context. Infrastructure can be swapped (e.g., from in-memory to DB, from REST to gRPC) without touching the domain. Prevents the common anti-patterns of anemic domain models and fat controllers.

### ADR-2: Java `record` for domain models and DTOs

**Decision:** All domain value objects and API response DTOs are Java `record` types.  
**Rationale:** Records are immutable by default, enforce all-fields constructors, and generate correct `equals`/`hashCode`/`toString`. The compact constructor enables invariant enforcement at construction time — no invalid objects can exist. Paired with Spring Boot 3.4's native record deserialization support.

### ADR-3: RFC 9457 `ProblemDetail` for all error responses

**Decision:** `GlobalExceptionHandler` returns `ProblemDetail` for all errors.  
**Rationale:** Standardizes error contracts across services. Consumers get consistent `status`, `title`, `detail`, and `type` fields. 500 handlers deliberately return generic messages to prevent internal detail leakage.

### ADR-4: JaCoCo 80% LINE coverage gate in CI

**Decision:** The `Quality Gate` pipeline stage blocks promotion if LINE coverage drops below 80%.  
**Rationale:** Prevents coverage regression as the service grows. Threshold is enforced in CI only (`jacocoTestCoverageVerification` task) to avoid blocking local dev builds.

### ADR-5: Swagger UI disabled in production

**Decision:** `application-prod.yml` disables both `/swagger-ui.html` and `/v3/api-docs`.  
**Rationale:** Interactive API documentation increases attack surface and enables endpoint enumeration by adversaries. Kept enabled in dev and staging for developer experience.

### ADR-6: Non-root container user

**Decision:** Dockerfile creates a dedicated `appuser` (UID 1001) and the K8s deployment enforces `runAsNonRoot: true`.  
**Rationale:** Defense in depth — container breakout exploits have reduced impact if the process doesn't run as root. Also enforced at the pod `securityContext` level with `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: true`, and `capabilities.drop: [ALL]`.

---

## Infrastructure & Deployment

### Environments

| Branch | Environment | Hostname |
|--------|-------------|----------|
| `main` | `prod` | `prod.app.local` |
| `release` | `staging` | `stg.app.local` |
| any other | `dev` | `dev.app.local` |

### Kustomize overlays

Environment-specific configuration is managed with Kustomize at `k8s/overlays/<env>/`:

- **`config.env`** — ConfigMap values (e.g., `APP_ENV=prod`)
- **`patch.yaml`** — resource overrides (CPU/memory requests and limits)
- **`ingress-patch.yaml`** — hostname and TLS settings
- **`hpa.yaml`** (prod only) — HorizontalPodAutoscaler (min 2 / max 5 replicas)
- **`pdb.yaml`** (prod only) — PodDisruptionBudget (min 1 available)

### Jenkins CI/CD pipeline

```
Checkout → Set Environment → Test → Quality Gate → Build & Push → Deploy
```

1. **Checkout** — `checkout scm`
2. **Set Environment** — derives `ENVIRONMENT`, `NAMESPACE`, `TAG` from branch name
3. **Test** — `./gradlew test` then `./gradlew bootJar` (single compilation cycle)
4. **Quality Gate** — `./gradlew jacocoTestCoverageVerification` (80% LINE gate)
5. **Build & Push** — `docker build` → push to private registry `192.168.0.106:5000`
6. **Deploy** — `kubectl apply -k k8s/overlays/<env>`

### Container

- Base image: `eclipse-temurin:21-jre-alpine` (smallest secure JRE image)
- On build: `apk upgrade --no-cache` patches all Alpine CVEs before packaging
- JVM flags: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=30.0`
- `/tmp` is mounted as `emptyDir` to allow `readOnlyRootFilesystem: true` (Spring Boot needs `/tmp` for nested JAR extraction)

---

## Observability

### Health endpoints (Spring Actuator)

| Path | Purpose |
|------|---------|
| `/actuator/health/liveness` | Container alive (Kubernetes `livenessProbe`) |
| `/actuator/health/readiness` | Ready for traffic (Kubernetes `readinessProbe` + `startupProbe`) |
| `/actuator/prometheus` | Prometheus scrape endpoint |
| `/actuator/info` | Build version and application info |

`show-details: never` is set globally — health endpoints never expose internal system state.

### Metrics (Micrometer + Prometheus)

All metrics carry three common tags configured in `MetricsConfig`:

| Tag | Source |
|-----|--------|
| `app` | `spring.application.name` |
| `environment` | `APP_ENV` env var (defaults to `dev`) |
| `version` | `build.version` from `build-info.properties` (generated by `springBoot.buildInfo()`) |

This enables Grafana dashboards to filter by environment without separate Prometheus instances.

### Structured logging

`logback-spring.xml` outputs JSON in all environments (via `logstash-logback-encoder`), ready for ingestion by Fluentd, Loki, or ELK.

---

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `spring.application.name` | `base-app` | App name; used as Prometheus tag |
| `app.environment` | `dev` | Set via `APP_ENV`; used as Prometheus tag |
| `server.port` | `8080` | HTTP port |
| `management.endpoint.health.show-details` | `never` | Never expose internal health detail |
| `springdoc.swagger-ui.enabled` | `true` (dev) / `false` (prod) | Swagger UI toggle |
| `springdoc.api-docs.enabled` | `true` (dev) / `false` (prod) | OpenAPI spec toggle |

Secrets (e.g., DB passwords, API tokens) are injected via Kubernetes Secrets and referenced in the deployment as `secretRef: name: app-secret`. Provisioned with `scripts/create-secrets.sh`.
