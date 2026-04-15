---
name: apply-audit-fixes
description: "Apply audit findings safely and incrementally. Use when: processing a project audit report, reducing technical debt before a release, fixing architectural violations, or applying security remediation. Input: an audit report with findings classified as critical/high/medium/low."
argument-hint: "Paste or reference the audit report"
---

# Skill: apply-audit-fixes

Apply audit findings safely, incrementally, and aligned with the project architecture defined in
[/docs/architecture.md](../../../docs/architecture.md) and [/docs/ai-context.md](../../../docs/ai-context.md).

---

## Source of Truth

Always load these files before touching any code:

| File | Purpose |
|------|---------|
| [/docs/architecture.md](../../../docs/architecture.md) | ADRs, layer rules, package layout, deployment topology |
| [/docs/ai-context.md](../../../docs/ai-context.md) | Code patterns, forbidden anti-patterns, testing standards |
| [/.github/copilot-instructions.md](../../copilot-instructions.md) | Critical DON'Ts, logging convention, build commands |

---

## PHASE 1 — Parse Audit Input

**Goal:** Understand the full scope before writing a single line of code.

1. Read the audit report provided as argument or attached file.
2. Extract every finding and tag it:

   | Severity | Action |
   |----------|--------|
   | `critical` | Must fix — blocks release |
   | `high` | Must fix — schedule immediately |
   | `medium` | Should fix — current cycle |
   | `low` | Skip unless explicitly requested |

3. Group findings by impacted layer:
   - `domain` — model invariants, port contracts
   - `application` — service orchestration, transaction scope
   - `infrastructure` — adapters, config, security headers
   - `cross-cutting` — logging, coverage, dependencies, CI

4. Flag any **ADR violation** explicitly:
   - ADR-1: Hexagonal layer dependency direction
   - ADR-2: `record` for domain models and DTOs
   - ADR-3: `ProblemDetail` for all error responses
   - ADR-4: JaCoCo 80% LINE coverage gate
   - ADR-5: Swagger UI disabled in production
   - ADR-6: Non-root container user and read-only filesystem

**Output of Phase 1:**
```
PARSED FINDINGS
───────────────
[CRITICAL] <id>: <one-line description> | Layer: <layer> | ADR: <adr if applicable>
[HIGH]     <id>: ...
[MEDIUM]   <id>: ...
[SKIPPED]  <id>: low priority — skipped
```

---

## PHASE 2 — Planning

**Model:** Claude Sonnet (planning) or Claude Opus (complex/ambiguous audits)

**Goal:** Build an executable plan before touching files.

Steps:
1. For each critical/high/medium finding, identify:
   - Exact files to change (use package layout from `architecture.md`)
   - Minimal change required (no refactors beyond scope)
   - Dependencies between fixes (order matters)
   - Risk level: `low` / `medium` / `high`

2. Group related fixes into atomic commits (one concern per commit).

3. Assess rollback path for each group:
   - Is the change reversible? (e.g., adding a field = easy, removing a port = risky)
   - Are there downstream consumers?

**Output of Phase 2:**
```
EXECUTION PLAN
──────────────
Step 1 — <group name>
  Files: src/main/java/com/baseapp/...
  Fixes: [CRITICAL-01], [HIGH-03]
  Risk: low
  Rollback: revert commit

Step 2 — ...

RISK ASSESSMENT
───────────────
- [HIGH] Changing port interface breaks all adapters implementing it
- [MEDIUM] Adding validation to domain record may break existing tests

ROLLBACK CONSIDERATIONS
───────────────────────
- All changes are in feature branch; revert via git revert <sha>
```

---

## PHASE 3 — Execution

**Model:** Claude Haiku (focused, minimal changes)

**Rules — non-negotiable:**

Refer to [Critical DON'Ts in /.github/copilot-instructions.md](../../copilot-instructions.md).

Additionally (skill-specific):
- Do NOT change files not listed in the plan.
- Do NOT introduce new patterns unless the audit explicitly requires it.

**Per-fix checklist:**

```
[ ] Change is minimal and isolated
[ ] Only files from the plan are modified
[ ] Diff shown and approved before applying
```

*(Layer integrity, ADR compliance, test coverage validation belong to Phase 4.)*

**Show a diff before applying each group.** Confirm or adjust before proceeding.

---

## PHASE 4 — Validation

**Model:** Claude Sonnet

**Goal:** Independent review of all applied changes.

Verify for each changed file:

1. **Layer integrity** — no import from a higher layer exists in a lower layer.
2. **ADR compliance** — no ADR is violated (check ADR-1 through ADR-6).
3. **Pattern consistency** — changes match the established code patterns in `ai-context.md`.
4. **Test coverage** — new/changed code has corresponding tests; run `./gradlew test`.
5. **No regressions** — existing tests still pass; run `./gradlew jacocoTestCoverageVerification`.
6. **Security** — no OWASP Top 10 issues introduced (injection, broken access control, info leakage).

Flag any issue found as:
- `BLOCKER` — must fix before merging
- `WARNING` — should fix; document if deferred
- `NOTE` — informational only

---

## PHASE 5 — Report

Generate the following report after all phases complete:

```markdown
# Fix Implementation Report

**Date:** <ISO date>
**Audit Source:** <file or description>
**Branch:** <git branch>

---

## Applied Fixes
| ID | Severity | Description | Files Changed | Commit |
|----|----------|-------------|---------------|--------|
| ... | CRITICAL | ... | ... | <sha> |

## Skipped Fixes
| ID | Severity | Reason |
|----|----------|--------|
| ... | LOW | Low priority — not requested |
| ... | MEDIUM | Out of scope for this cycle |

## Risks
- <risk description and mitigation>

## Validation Notes
- <BLOCKER/WARNING/NOTE items from Phase 4>

## Next Steps
- <remaining work or deferred items>
```

---
