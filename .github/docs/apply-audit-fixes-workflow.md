# Workflow: apply-audit-fixes

Step-by-step guide for applying project audit findings using the
[apply-audit-fixes skill](../skills/apply-audit-fixes/SKILL.md).

---

## When to Use

| Trigger | Description |
|---------|-------------|
| **After an audit** | Security audit, architecture review, or code quality report has been received |
| **Before a release** | Pre-release hardening pass to clear critical and high findings |
| **During a refactoring cycle** | Use audit as the prioritized backlog for incremental improvements |
| **After dependency updates** | Re-evaluate findings that may have been introduced by new versions |
| **CI quality gate failure** | Coverage drops or architectural lint fails — re-run audit workflow |

---

## Prerequisites

- Git branch checked out (never apply directly to `main`)
- Audit report available (file path or raw content)
- Project builds cleanly: `./gradlew test`
- Access to [/docs/architecture.md](../../docs/architecture.md) and [/docs/ai-context.md](../../docs/ai-context.md)

---

## Step-by-Step with Claude / Copilot

### Step 1 — Open a feature branch

```bash
git checkout -b fix/audit-<YYYY-MM-DD>
```

### Step 2 — Run the prompt

Open agent chat and run:

```
/apply-audit-fixes-run

Audit report: <paste content or reference file path>
```

The prompt is at [.github/prompts/apply-audit-fixes-run.prompt.md](../prompts/apply-audit-fixes-run.prompt.md).

### Step 3 — Review the Execution Plan (Phase 2 output)

Before any code changes, the agent outputs:

- Grouped fix steps with impacted files
- Risk assessment per group
- Rollback strategy

**Approve or adjust the plan before proceeding.**

### Step 4 — Review each diff (Phase 3)

For every fix group the agent will show a full diff.  
Only confirm after verifying:

- [ ] Changed files are in the plan
- [ ] Layer boundaries are preserved
- [ ] No unrelated files touched
- [ ] Change is minimal

### Step 5 — Validate (Phase 4)

```bash
./gradlew test
./gradlew jacocoTestCoverageVerification
```

Address any BLOCKER before opening a PR.

### Step 6 — Generate and save the Fix Report (Phase 5)

Save the report to the repository:

```
docs/audit-fixes-<YYYY-MM-DD>.md
```

### Step 7 — Open a Pull Request

Use the Fix Implementation Report as the PR description body.  
Link to the original audit document in the PR.

---

## PR Workflow Integration

```
feature branch
    ↓
apply-audit-fixes workflow
    ↓  (all critical/high resolved, tests green, coverage ≥ 80%)
Pull Request → review → merge to main / release
    ↓
Jenkins CI: Test → Quality Gate → Build & Push → Deploy
```

Label the PR with `audit-fix` for traceability.

---

## CI/CD Integration (Optional)

### Block promotion on critical findings

Add a stage to the Jenkins pipeline that fails if a new audit report contains
unresolved critical findings:

```groovy
stage('Audit Gate') {
    steps {
        // Run audit tool (e.g., Trivy, Checkstyle, custom script)
        // Fail if critical findings exist in the report
        sh './scripts/check-audit.sh audit-report.json'
    }
}
```

> **Note:** `check-audit.sh` is a placeholder. Implement it to parse your audit tool's output format and exit non-zero on unresolved critical findings.

### Automate low-risk fixes (Future)

For low-risk, mechanical fixes (e.g., Checkstyle formatting, dependency upgrades),
the workflow can run headlessly in CI using the prompt as an agent task.  
Always gate the result with a human PR review before merging.

---

## Severity Handling

See [Phase 1 of the skill](../skills/apply-audit-fixes/SKILL.md) for severity classification definitions.

---

## Guardrails

- **Never apply directly to `main`** — always use a branch.
- **Never skip Phase 2 (planning)** — the diff review in Phase 3 depends on it.
- **Never batch unrelated fixes** — one concern per commit, one commit per fix group.
- **The 80% LINE coverage gate must stay green** — run `jacocoTestCoverageVerification` before PR.
- **ADRs are law** — a fix that violates an ADR requires a new ADR to supersede it first.

---
