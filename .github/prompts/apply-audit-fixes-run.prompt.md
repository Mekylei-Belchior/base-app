---
description: "Apply fixes from an audit report using the apply-audit-fixes skill"
agent: agent
argument-hint: "Path or content of the audit report"
---

Apply audit findings to this project using the skill defined in
[.github/skills/apply-audit-fixes/SKILL.md](../skills/apply-audit-fixes/SKILL.md).

The skill file contains the source of truth references. Read it first.

## Audit Report

**Auto-discovery:** The agent MUST scan `docs/audits/` for all `*.md` files, sort by
filename date descending (convention: `project-audit-YYYY-MM-DD-HHmmss.md`), and select the
most recent report.

**Pre-flight checks (agent MUST complete before proceeding):**

1. List all files in `docs/audits/`.
2. If the directory is empty or contains no `.md` files → **STOP**. Tell the user:
   _"No audit reports found in `docs/audits/`. Generate one first with the project-audit skill."_
3. Identify the most recent report by filename date.
4. Present the selection to the user:
   _"I found N report(s). The most recent is `docs/audits/<filename>` (<date>). Should I proceed with this report?"_
5. **Wait for explicit user confirmation** before continuing to the Instructions.
6. If the user selects a different report, use that one instead.
7. Read the confirmed report in full before moving to Phase 1.

## Instructions

1. **Read** [.github/skills/apply-audit-fixes/SKILL.md](../skills/apply-audit-fixes/SKILL.md) fully before proceeding.

2. **Phase 1 — Parse:** Extract all findings. Group by severity (critical / high / medium).
   Skip low priority unless I explicitly ask for them.

3. **Phase 2 — Plan:** Output a step-by-step execution plan with impacted files,
   fix groupings, risk level, and rollback notes. Wait for my approval before continuing.

4. **Phase 3 — Execute:** For each planned group, show the **full diff first**.
   Apply only after I confirm. One group at a time.

5. **Phase 4 — Validate:** Review all changes for ADR compliance, layer integrity,
   and test coverage. Run `./gradlew test` and `./gradlew jacocoTestCoverageVerification`.
   Report any BLOCKER or WARNING before declaring done.

6. **Phase 5 — Report:** Generate the Fix Implementation Report as specified in the skill.

All constraints from the skill apply. Do not deviate.
