# Issue -> Branch -> Commit -> PR Prompt

Harness version: v1 (2026-06-28)

Follow `AGENTS.md` and `HARNESS.md`.

You are operating inside the GJLearn Backend repository. Treat the task file as the source of truth for scope.

## Required process

1. Read the task file completely.
2. Read every file listed under `Read first`.
3. Inspect GitHub issue/PR/template context from `harness/runs/<run-id>/github-context.md` when present. Match the repository's observed issue/PR sections before inventing a new format.
4. Inspect current git state before editing.
5. Search for neighboring code and tests before changing code. Trace controller -> service -> repository -> DTO/test conventions rather than guessing imports or API shapes.
6. Modify only files allowed by the task packet.
7. Run focused tests for the touched domain, then `scripts/harness/verify.sh` after changes unless the task explicitly narrows verification.
8. Update the PR handoff draft when the task creates a feature branch/PR: keep the repository's basic PR sections, include validation evidence, and add Mermaid only when it helps explain API/domain/data flow.
9. Produce final JSON matching `harness/schemas/task-result.schema.json`.

## Completion rules

- Do not claim tests passed without command output.
- Do not commit, push, deploy, or edit secrets unless the task explicitly permits it.
- If verification fails, report the exact command and failure summary.
- If the task touches monitoring or deployment, include rollback/risk notes in `risks`.
- Include the harness version in final `next_steps` or reviewer notes when relevant.
- Treat the task packet `Guardrails` as mandatory. If a requested action violates them, stop and report a blocker.
- Do not include raw secrets or private env values in final JSON.
- Do not claim an issue or PR was created unless the `gh` command returned the number/URL.
- Do not create broad commits. Stage small logical groups and use the repository conventional commit style when commit is explicitly permitted.
- Mermaid diagrams, when used, must describe the actual implemented flow/entities. Replace placeholder node names before PR creation.

## Final JSON fields

Return only JSON with these fields:

- `status`
- `summary`
- `files_changed`
- `verification`
- `blockers`
- `risks`
- `next_steps`
