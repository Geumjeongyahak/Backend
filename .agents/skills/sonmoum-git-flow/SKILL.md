---
name: sonmoum-git-flow
description: Apply the Sonmoum API repository's branch naming, commit message, and pre-commit workflow conventions. Use when Codex needs to create a new branch, decide how to split completed work into commits, draft or validate Conventional Commit messages, reconcile the documented git rules with the repository's real history, or perform the final git hygiene checks before committing backend changes in this project.
---

# Sonmoum Git Flow

Use this skill to turn completed Sonmoum backend work into a clean branch and commit history that matches the repository's documented standards as closely as possible.

Read [project-git-conventions.md](./references/project-git-conventions.md) before making naming decisions if the request is ambiguous or if the observed history conflicts with the written rules.

## Workflow

### 1. Classify the work unit

Inspect the current change before proposing git actions.

- Run `git status --short`.
- If the change is broad, run `git diff --stat` and group files into coherent commit units.
- Identify:
  - issue number if known
  - change type: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
  - primary domain scope: `user`, `lesson`, `subject`, `student`, `classroom`, `request`, `auth`, `global`

Do not force one giant commit when unrelated work is mixed together. Propose a split first.

### 2. Pick the branch name

Default to the documented branch prefixes:

- `feature/{issue-number}-{slug}` for new functionality
- `fix/{issue-number}-{slug}` for bug fixes
- `hotfix/{issue-number}-{slug}` for urgent production fixes

Use lowercase ASCII slug segments joined by hyphens.

The repository history also contains `feat/{issue}-{slug}` branches. Treat that as a compatibility pattern, not the default target. Only mirror it when the user explicitly asks to follow an existing remote branch family.

Use `./scripts/propose_branch_name.sh` when you need a deterministic proposal.

Examples:

```bash
./scripts/propose_branch_name.sh feature 24 lesson-calendar-api
./scripts/propose_branch_name.sh fix 31 request-approval-null-check
```

### 3. Draft the commit message

Default to this shape:

```text
<type>(<scope>): <subject>
```

Rules:

- Always include scope unless there is a clear reason not to.
- Keep the subject to one line.
- Prefer concise Korean subjects because the current history is predominantly Korean.
- Describe the user-visible or codebase-visible change, not the implementation detail alone.
- Do not capitalize mechanically. Natural Korean phrasing is fine.
- Add issue linkage in the footer only when the workflow already uses it, for example `Refs #24` or `Closes #24`.

Use `./scripts/propose_commit_message.sh` to normalize and validate the line.

Examples:

```bash
./scripts/propose_commit_message.sh feat lesson "수업 캘린더 조회 API 추가"
./scripts/propose_commit_message.sh fix auth "로그인 만료 토큰 처리 수정" 31
```

### 4. Validate before committing

Before creating the commit:

- Ensure the branch matches the intended task.
- Ensure the staged diff only contains one coherent unit of work.
- Run the narrowest relevant verification command first.
  - Java/Spring backend changes: targeted test class if practical, otherwise `./gradlew test`
  - docs-only changes: skip heavy test runs and state that explicitly
- Re-read the final commit line and check that `type`, `scope`, and subject agree with the actual diff.

Use `./scripts/validate_git_flow.sh` for a quick regex-level check of branch and commit naming.

### 5. Communicate the result

When reporting back to the user:

- state the chosen branch
- state the final commit message
- mention what validation was run
- mention any mismatch you intentionally accepted between repo history and written convention

## Decision Notes

- Prefer written project convention over inconsistent historical shortcuts.
- Prefer one meaningful commit over many micro-commits.
- Prefer several coherent commits over one mixed commit.
- If the worktree is already dirty with unrelated user changes, do not stage or commit them implicitly.
- If there is no issue number, say that explicitly and use a placeholder only if the user requests it.
