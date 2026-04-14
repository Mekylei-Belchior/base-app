# Workflow: project-audit

> Describes when and how to run the `project-audit` skill, and how to integrate it into CI/CD pipelines.

---

## When to Use

| Trigger | Description |
|---------|-------------|
| **General audit** | Periodic health check of the repository (recommended: once per sprint or monthly) |
| **Pre-release** | Before merging to `release` or `main` branch — append pre-release flag to prompt |
| **Architecture review** | Before adding a new layer, introducing a new adapter, or changing an ADR |
| **New team member onboarding** | Run audit and share report so engineers understand current quality baseline |
| **After a dependency upgrade** | Spring Boot, Gradle, or JDK version bumps can silently break patterns |
| **Post-incident** | After a production incident, audit CI/CD and infrastructure sections |

---

## How to Use with Claude Code

### Step 1 — Open the repository

```bash
cd <repository-root>
# Open VS Code with Copilot Chat in Agent mode
```

### Step 2 — Paste the full prompt

Open a new Copilot Chat in Agent mode and paste the full prompt from:

```
.ai/prompts/project-audit-run.md
```

Use the fenced block labeled `## Prompt` — everything between the triple backticks.

### Step 3 — Let the agent run all four stages

The agent will:
1. Read all four source-of-truth files (Stage 1 — Opus)
2. Examine all source files across the six audit areas (Stage 2 — Haiku)
3. Consolidate and deduplicate findings (Stage 3 — Sonnet)
4. Generate the full markdown report (Stage 4 — Sonnet)

Do not interrupt between stages. The agent will produce a continuous output.

### Step 4 — Save and version the report

```bash
# Save report with timestamp
mkdir -p docs/audits
# Copy the output from Claude Code into a file:
# docs/audits/audit-YYYY-MM-DD.md
git add docs/audits/
git commit -m "docs: add project audit report YYYY-MM-DD"
```

### Step 5 — Act on findings

- CRITICAL findings: must be resolved before next release
- Context violations: create a dedicated ticket; assign to the owning squad
- HIGH findings: schedule for current or next sprint
- MEDIUM/LOW: add to backlog with appropriate priority

---

## Scoped Audits (single area)

To audit only one area, append to the prompt:

```
— Scope this audit to the [Infrastructure & Docker] section only.
```

Replace `[Infrastructure & Docker]` with any area name from the Stage 1 table in `.ai/skills/project-audit.md`.

---

## Integration: Pull Request Checks

### GitHub Actions

Add to `.github/workflows/pr-audit.yml`:

```yaml
name: AI Project Audit (manual)

on:
  workflow_dispatch:
    inputs:
      scope:
        description: "Audit scope (leave blank for full audit)"
        required: false
        default: ""

jobs:
  audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run project-audit skill
        # TODO: Replace with your organization's approved Claude API integration.
        # Prompt source: .ai/prompts/project-audit-run.md
        # Output target: docs/audits/audit-$(date +%F).md
        run: |
          echo "PLACEHOLDER — wire your Claude API integration here"

      - name: Upload audit report
        uses: actions/upload-artifact@v4
        with:
          name: audit-report
          path: docs/audits/
```

---

## Integration: Jenkins Pipeline

Add a manual or post-build stage to `Jenkinsfile`:

```groovy
stage('AI Audit') {
    when {
        anyOf {
            branch 'main'
            branch 'release'
            expression { return params.RUN_AUDIT == true }
        }
    }
    steps {
        script {
            // TODO: Replace with your organization's approved Claude API wrapper.
            // Do NOT inline API keys or use raw curl in production pipelines.
            // Example: sh "your-org-cli audit --prompt .ai/prompts/project-audit-run.md --output docs/audits/audit-${BUILD_NUMBER}.md"
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'docs/audits/*.md', allowEmptyArchive: true
        }
    }
}
```

---

## File Map

| File | Purpose |
|------|---------|
| `.ai/skills/project-audit.md` | Skill — stages, severity definitions, rules, report template |
| `.ai/prompts/project-audit-run.md` | Prompt — copy-paste trigger; configurable with `Additional instruction:` modifiers |
| `.ai/workflows/project-audit-workflow.md` | Workflow — when/how to run, CI/CD integration |

---

## Governance

- Audit reports in `docs/audits/` must be reviewed by the tech lead before findings are closed.
- Context violations must never be dismissed without updating the corresponding ADR in `docs/architecture.md` or DON'T in `docs/ai-context.md`.
- When source-of-truth documents change, future audit runs automatically reflect updated constraints — no manual changes to `.ai/skills/project-audit.md` are needed.
- If an ADR is changed, add a note to it with the audit date and ticket reference.
