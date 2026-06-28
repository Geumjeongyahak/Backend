# Issue -> Branch -> Commit -> PR Prompt

Harness version: v1-2026-06-28

Follow `AGENTS.md` and `HARNESS.md`.

You are operating inside the GJLearn Backend repository. Treat the task file as the source of truth for scope.

## Required process

1. Read the task file completely.
2. Read every file listed under `Read first`.
3. Inspect current git state before editing.
4. Modify only files allowed by the task packet.
5. Run `scripts/harness/verify.sh` after changes unless the task explicitly narrows verification.
6. Produce final JSON matching `harness/schemas/task-result.schema.json`.

## Completion rules

- Do not claim tests passed without command output.
- Do not commit, push, deploy, or edit secrets unless the task explicitly permits it.
- If verification fails, report the exact command and failure summary.
- If the task touches monitoring or deployment, include rollback/risk notes in `risks`.
- Include the harness version in final `next_steps` or reviewer notes when relevant.

## Final JSON fields

Return only JSON with these fields:

- `status`
- `summary`
- `files_changed`
- `verification`
- `blockers`
- `risks`
- `next_steps`
