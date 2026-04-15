---
name: project-audit
description: "Perform a complete technical audit of this repository. Use when: periodic health check, pre-release validation, architecture review, or post-incident analysis. Multi-model strategy: Opus for planning, Haiku for execution, Sonnet for consolidation."
---

# Skill: project-audit

> Performs a complete technical audit of this repository using a multi-model strategy.  
> Always treats `/docs/architecture.md`, `/docs/ai-context.md`, and `/.github/copilot-instructions.md` as the source of truth.

**Version:** 1.0.0

---

## Vision

**Objective:** Produce a prioritized, evidence-based technical assessment that identifies architecture violations, code quality gaps, build/CI risks, infrastructure weaknesses, and testing blind spots.

**Scope:** All version-controlled files in the repository.  
**Out of scope:** Generated files under `build/`, binary artifacts, IDE-specific files.

---

## Architecture: Multi-Model Strategy

### STAGE 1 — Planning (Opus)

**Goal:** Understand the system holistically and produce a structured analysis plan.

Tasks:
1. Read `/docs/architecture.md` in full — internalize:
   - Hexagonal architecture layers and their invariants
   - All ADRs (ADR-1 through ADR-6)
   - Package layout (`com.baseapp.*`)
   - Technology stack (Java 21, Spring Boot 3.4, Gradle 9.4.1, k3s/Kustomize, Jenkins, Trivy, JaCoCo, Micrometer)
2. Read `/docs/ai-context.md` in full — internalize:
   - All critical DON'Ts
   - Code patterns for records, ports, services, controllers, DTOs, exception handlers, logging
   - Testing patterns (`@ExtendWith(MockitoExtension.class)`, `@WebMvcTest`, `@MockitoBean`)
3. Read `/.github/copilot-instructions.md` — captures live coding constraints
4. Identify high-risk areas based on architecture complexity
5. Produce an **Analysis Plan** with exactly these six areas:

| Area | Key questions |
|------|--------------|
| **Structure** | Are layer boundaries intact? Any cross-layer import? |
| **Code Quality** | Pattern compliance? DON'Ts violated? Logging correct? |
| **Build & Dependencies** | Dependency hygiene? Wrapper version pinned? Coverage gate present? |
| **CI/CD** | Pipeline stages complete? Quality gate enforced? Trivy scan present? |
| **Infrastructure & Docker** | Non-root user? Read-only root FS? Kustomize overlays consistent? |
| **Testing** | Coverage ≥ 80%? All tests have assertions? No `@MockBean` usage? |

---

### STAGE 2 — Execution (Haiku)

**Goal:** Analyze each area independently; collect raw findings with file/line evidence.

For each area, Haiku must:
1. List every file examined
2. Tag each finding with severity using the definitions below
3. Include the exact file path + line number or code snippet as evidence
4. Flag explicitly whether it is a **context violation** (contradicts source-of-truth docs)

**Severity definitions:**

| Severity | Definition |
|----------|------------|
| `CRITICAL` | Production risk or security vulnerability. Blocks release. |
| `HIGH`     | ADR or pattern violation with significant quality impact. Fix this sprint. |
| `MEDIUM`   | Non-compliance that does not cause immediate harm. Fix in next sprint. |
| `LOW`      | Minor inconsistency or improvement opportunity. Add to backlog. |

> **Checklist source:** The source-of-truth documents (`docs/architecture.md`, `docs/ai-context.md`, `.github/copilot-instructions.md`) define the canonical checklist. The checks below are primary anchors — they do not replace reading those documents. Any DON'T or ADR not listed here must still be checked.

#### Area: Structure
- Verify no `infrastructure.*` import exists in `domain.*` or `application.*`
- Verify no Spring annotations (`@Service`, `@Component`, `@Autowired`, `@Transactional`) appear in `domain.*`
- Verify domain model classes are `record` types only
- Verify ports are plain Java interfaces with no Spring annotations
- Verify `application.service` classes implement the corresponding `port.in` interface

#### Area: Code Quality
- Check all services use constructor injection (no `@Autowired` on fields)
- Check controllers inject use case ports, not services directly
- Check controllers never return domain objects (always DTOs)
- Check DTO records use static `from()` factory methods
- Check record accessors use no `get` prefix (e.g., `record.name()` not `record.getName()`)
- Check all logging uses SLF4J (`LoggerFactory.getLogger`) — zero `System.out.println`
- Check `GlobalExceptionHandler` uses `ProblemDetail` for all errors
- Check 500 handlers never call `e.getMessage()` — must return generic message
- Check business logic is absent from controllers and adapters

#### Area: Build & Dependencies
- Verify `build.gradle` contains `jacocoTestCoverageVerification` with `minimum = 0.80` on LINE counter
- Verify `gradle-wrapper.properties` pins a specific Gradle version (9.4.1)
- Verify no snapshot or unresolved dynamic versions in dependencies
- Check that `bootJar` task produces `base-app.jar`

#### Area: CI/CD
- Verify `Jenkinsfile` contains stages: `Checkout → Set Environment → Test → Quality Gate → Build & Push → Deploy`
- Verify Quality Gate stage runs `jacocoTestCoverageVerification`
- Verify Trivy scan is invoked before image push
- Verify the pipeline uses branch-to-environment mapping consistent with `architecture.md` (`main→prod`, `release→staging`, others→`dev`)

#### Area: Infrastructure & Docker
- Verify `Dockerfile` creates a non-root user with UID 1001 (`appuser`)
- Verify base image is `eclipse-temurin:21-jre-alpine`
- Verify `k8s/base/deployment.yaml` has `securityContext` with `runAsNonRoot: true`, `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: true`, `capabilities.drop: [ALL]`
- Verify all three overlays (dev, staging, prod) have `namespace.yaml` and `kustomization.yaml`
- Verify prod overlay has `hpa.yaml` (min 2, max 5) and `pdb.yaml`
- Verify ADR-5: `application-prod.yml` disables swagger-ui and `/v3/api-docs`

#### Area: Testing
- Verify all service tests use `@ExtendWith(MockitoExtension.class)` with no Spring context
- Verify all controller tests use `@WebMvcTest` and `@MockitoBean` (not `@MockBean`)
- Verify test method naming follows `method_shouldBehavior_whenCondition()`
- Verify no test class is missing assertions
- Verify reported JaCoCo LINE coverage meets 80% gate

---

### STAGE 3 — Consolidation (Sonnet)

**Goal:** Merge raw findings from Stage 2 into a coherent, deduplicated, prioritized list.

Tasks:
1. Remove duplicate findings that reference the same root cause
2. Group related findings under the appropriate audit area
3. Escalate severity if a finding violates an ADR from `architecture.md`
4. Cross-reference all findings against source-of-truth documents
5. Identify which findings are **context violations** (explicit contradiction of documented architecture or patterns)

#### CONTEXT VIOLATIONS — mandatory section

A finding is a **context violation** if it:
- Contradicts an ADR defined in `docs/architecture.md`
- Contradicts a DON'T listed in `docs/ai-context.md`
- Contradicts a rule in `/.github/copilot-instructions.md`

Context violations must be escalated to at least `HIGH` and listed separately with:
- Reference to the specific ADR/rule violated
- File + evidence
- Recommended fix

---

### STAGE 4 — Final Report (Sonnet)

**Goal:** Produce a complete, actionable markdown report.

Output structure:

```markdown
# Project Audit Report

**Repository:** base-app  
**Date:** <ISO 8601 datetime with format YYYY-MM-DD-HHmmss>
**Audited by:** project-audit skill (Opus + Haiku + Sonnet)

---

## Executive Summary
- Overall health rating: HEALTHY | NEEDS ATTENTION | CRITICAL
- Total findings: X (CRITICAL: n, HIGH: n, MEDIUM: n, LOW: n)
- Context violations: n
- Coverage status: X% (gate: 80%)

---

## Architecture Review
> Layer compliance, ADR adherence, Hexagonal Architecture integrity.

### Findings
| Severity | File | Line | Finding | ADR |
|----------|------|------|---------|-----|

---

## Code Quality
> Pattern compliance, DON'Ts enforcement, logging, exception handling.

### Findings
| Severity | File | Line | Finding |
|----------|------|------|---------|

---

## Build & Dependencies
> Gradle configuration, dependency health, coverage gate.

### Findings
| Severity | File | Area | Finding |
|----------|------|------|---------|

---

## CI/CD Pipeline
> Jenkins pipeline stages, quality gate, Trivy, branch strategy.

### Findings
| Severity | File | Stage | Finding |
|----------|------|-------|---------|

---

## Infrastructure & Docker
> Container security, Kubernetes manifests, Kustomize overlays.

### Findings
| Severity | File | Finding |
|----------|------|---------|

---

## Testing Strategy
> Coverage, test patterns, assertion quality, Spring context usage.

### Findings
| Severity | File | Finding |
|----------|------|---------|

---

## Key Risks
> Top 3–5 risks that could cause production incidents or block releases.

1.
2.
3.

---

## Context Violations
> Findings that explicitly contradict source-of-truth documentation.  
> Each entry carries a CV-N identifier. Area sections reference these IDs instead of duplicating the finding.

| ID | Severity | File | Violated Rule | Evidence | Fix |
|----|----------|------|--------------|---------|-----|

---

## Prioritized Recommendations
> Ordered by impact. Each item is actionable.

| Priority | Area | Action | Effort |
|----------|------|--------|--------|
| P1 | | | |
| P2 | | | |

---

## Suggested Roadmap

> Each item must reference a specific finding from this report. Do not use generic categories as action items.

### Immediate (this sprint)
- [ ] `<CRITICAL finding: area — file — action>`
- [ ] `<Context violation: CV-N — violated rule — action>`

### Short-term (1–2 sprints)
- [ ] `<HIGH finding: area — file — action>`

### Medium-term (next quarter)
- [ ] `<MEDIUM finding: area — file — action>`

---
_Generated by project-audit skill v1.0.0. Source of truth: `/docs/architecture.md`, `/docs/ai-context.md`, `/.github/copilot-instructions.md`_
```

### Output Contract

The final report MUST be persisted as a markdown file following the rules below:

#### File Name
Format: `project-audit-<ISO 8601 datetime with format YYYY-MM-DD-HHmmss>.md`  
Example: `project-audit-2026-04-14-153000.md`
Where:
- `YYYY-MM-DD-HHmmss` must match the same timestamp used in the report header

#### File Path
Location: `docs/audits/`  
Example: `docs/audits/project-audit-2026-04-14-153000.md`

#### Rules
1. The directory `docs/audits` MUST be created if it does not exist
2. The file MUST be written (not just printed)
3. Do not overwrite existing files — always generate a new timestamp
4. The file content MUST exactly match the Final Report output

---

## Skill Rules

1. **Source of truth is absolute.** Never contradict `architecture.md`, `ai-context.md`, or `copilot-instructions.md` without explicit justification.
2. **Evidence is mandatory.** Every finding must cite a file path and line number or code snippet. No generalities.
3. **Be critical.** If something is wrong, say it clearly. Do not soften findings.
4. **No duplication.** Each finding appears once. Context violations appear exclusively in the Context Violations section; area sections reference them by ID (e.g., "→ CV-1") rather than repeating the full finding.
5. **Severity must be justified.** State why a finding is CRITICAL vs HIGH vs MEDIUM.
6. **Context violations get their own section.** They are not buried in area sections.
7. **Recommendations must be actionable.** "Improve code quality" is not acceptable. "Replace `@Autowired` field injection in `HelloService` with constructor injection" is.
8. **Zero-finding areas are valid.** If an area has no findings, state "No findings" explicitly. Do not fabricate findings to fill report sections.

---

## Model Assignment (recommended for orchestrated usage)

> When running in a single chat session, one model executes all stages. The stage structure remains a useful reasoning scaffold regardless.  
> When using a multi-agent orchestration layer, assign models as follows:

| Stage | Recommended Model | Rationale |
|-------|------------------|-----------|
| 1 — Planning | Opus | Deep reasoning over architecture docs; establishes the analysis frame |
| 2 — Execution | Haiku | High-volume, low-cost, precise pattern matching across many files |
| 3 — Consolidation | Sonnet | Judgment-intensive deduplication and severity calibration |
| 4 — Report | Sonnet | Long-form structured writing with full context |
