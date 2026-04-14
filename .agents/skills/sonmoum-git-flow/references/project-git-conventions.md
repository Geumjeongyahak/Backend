# Sonmoum Git Conventions

## Source of truth

This skill is based on two signals:

1. `AGENTS.md` and `docs/convention/convention.md`
2. The recent git history and branch names in the repository

When they disagree, prefer the written convention unless the user asks to stay compatible with an existing live branch pattern.

## Documented standard

### Commit message

Use Conventional Commits:

```text
<type>(<scope>): <subject>
```

Supported `type` values:

- `feat`
- `fix`
- `docs`
- `style`
- `refactor`
- `test`
- `chore`

Supported default `scope` values:

- `user`
- `lesson`
- `subject`
- `student`
- `classroom`
- `request`
- `auth`
- `global`

### Branch name

The docs define:

- `feature/{issue-number}-{feature-name}`
- `fix/{issue-number}-{bug-description}`
- `hotfix/{issue-number}-{description}`

## Observed repository history

Recent history shows these patterns:

### Commits

- Many commits omit scope: `feat: ...`
- Some commits include scope: `fix(classroom): ...`
- Subjects are usually written in Korean.
- Issue linkage often appears as `(#14)` or `(#17)` in the subject line.

### Branches

- Remote history contains `origin/feat/14-lesson`, `origin/feat/17-subject`, `origin/feat/20-lesson-cud`
- Local history also contains `feature/18-request`

## Recommended policy for future work

Use this policy unless the user directs otherwise:

1. Use documented branch prefixes: `feature/`, `fix/`, `hotfix/`.
2. Use Conventional Commit with scope by default: `<type>(<scope>): <subject>`.
3. Keep Korean commit subjects if the rest of the branch history is Korean.
4. Put issue references in a footer or PR context instead of overloading the subject line when possible.
5. Treat old `feat/` branch names and scope-less commit lines as legacy compatibility, not the preferred default.

## Scope mapping hints

- `src/main/java/geumjeongyahak/domain/lesson/**` -> `lesson`
- `src/main/java/geumjeongyahak/domain/subject/**` -> `subject`
- `src/main/java/geumjeongyahak/domain/student/**` -> `student`
- `src/main/java/geumjeongyahak/domain/classroom/**` -> `classroom`
- `src/main/java/geumjeongyahak/domain/request/**` -> `request`
- `src/main/java/geumjeongyahak/domain/auth/**` -> `auth`
- `src/main/java/geumjeongyahak/domain/users/**` -> `user`
- `src/main/java/geumjeongyahak/common/**` or build/config files -> `global`

If multiple domains are changed, either split the commit or use `global` only when the change is truly cross-cutting.
