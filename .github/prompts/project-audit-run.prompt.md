---
description: "Run a full project audit using the project-audit skill"
agent: agent
---

# Prompt: project-audit-run

> Ready-to-use prompt to trigger the `project-audit` skill. Copy and paste as-is into Claude Code or any Claude chat with repository access.

---

## Prompt

```
You are a Staff Engineer performing a complete technical audit of this repository.

MANDATORY — Read these files before doing anything else:
1. Read /.github/skills/project-audit/SKILL.md       — full audit skill and rules
2. Read /docs/architecture.md                         — system architecture, ADRs, package layout
3. Read /docs/ai-context.md                           — code patterns, DON'Ts, testing patterns
4. Read /.github/copilot-instructions.md              — coding constraints (source of truth)

These four files are your source of truth. Do not contradict them without explicit justification.

EXECUTION — Follow the four-stage process defined in `/.github/skills/project-audit/SKILL.md` exactly.
Do not skip or reorder stages. Produce the analysis plan (Stage 1) before examining any files.
Every finding must include a file path and evidence. Every recommendation must be actionable.

OUTPUT FORMAT:
- Full markdown report using the template in the skill
- No findings omitted
- Context violations appear in the Context Violations section only; area sections reference them by CV-N ID
- Severity ratings must be justified using the definitions in the skill
```

---

## Quick Invocation (one-liner)

```
Run the project-audit skill at /.github/skills/project-audit/SKILL.md against this entire repository. Use /docs/architecture.md, /docs/ai-context.md, and /.github/copilot-instructions.md as source of truth. Produce the full audit report.
```

---

## Parameters you can append

| Option | Add to end of prompt |
|--------|----------------------|
| Scope to one area only | `Additional instruction: Scope this audit to [Area Name] only.` |
| Focus on context violations | `Additional instruction: Expand and prioritize only the Context Violations section.` |
| Short summary only | `Additional instruction: Produce only the Executive Summary and Prioritized Recommendations sections.` |
| Pre-release check | `Additional instruction: This is a pre-release audit. Treat all HIGH findings as CRITICAL blockers.` |
