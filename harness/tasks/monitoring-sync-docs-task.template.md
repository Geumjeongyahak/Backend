# Task: 모니터링/동기화/문서화 변경

## Goal

금정야학 백엔드의 모니터링, 동기화, 배포 문서 변경을 작은 PR 단위로 구현하고 검증한다.

## Issue

- GitHub issue: #<issue-number>

## Read first

- `AGENTS.md`
- `HARNESS.md`
- `harness/v1/2026-06-28/guide.md`
- `README.md`
- `DEPLOY.md`
- `docs/deployment/gcloud-provisioning.md`
- `docs/issue/infra-low-cost-gce-homelab-observability.md`
- 관련 script 또는 config 파일

## Modify

Allowed examples. 실제 작업에 필요한 최소 파일만 수정한다.

- `README.md`
- `DEPLOY.md`
- `docs/deployment/*.md`
- `infra/monitoring/**`
- `scripts/monitoring/**`
- `scripts/gcp/**`
- `src/main/resources/logback-spring.xml`
- `HARNESS.md`
- `harness/**`
- `scripts/harness/**`

## Constraints

- `.env`, `*.env`, secrets, credentials는 수정하지 않는다.
- 홈랩 운영 경로 `/home/min/Infra/monitoring`는 task가 명시하지 않으면 직접 변경하지 않는다.
- deploy/apply/destructive command는 실행하지 않는다.
- Cloud/GitHub/Tailscale secrets 값은 출력하지 않는다.
- Codex/Hermes self-report만으로 완료 처리하지 않는다.

## Guardrails

- Scope: `Modify`에 적힌 경로 밖을 바꾸지 않는다.
- Secrets: `.env`, `*.env`, credential, token 값을 prompt/result/artifact에 넣지 않는다.
- Commands: test/lint/diff는 허용하지만 deploy, sudo, force push, destructive cleanup은 별도 승인 없이는 실행하지 않는다.
- Verification: runner가 실행한 검증 결과가 Codex self-report보다 우선한다.

## Harness team

- Harness Owner: workflow와 v1/v2 구조 결정.
- Prompt Librarian: task packet과 prompt recipe 유지.
- Runner Engineer: hook, schema, verifier 수정.
- Verifier: 실제 command output으로 완료 판정.
- Reviewer/Operator: PR risk와 외부 side effect 승인.

## Acceptance criteria

- 변경 목적이 issue와 PR 본문에서 추적 가능하다.
- 문서와 script/config가 서로 같은 운영 정책을 말한다.
- prompt/result artifact를 만들 수 있는 harness path가 있다.
- 검증 명령과 exit code가 기록된다.
- 실패나 미검증 항목은 blocker로 남긴다.

## Verification

Run:

- `scripts/harness/verify.sh`
- `scripts/harness/summarize-diff.sh`

If full Gradle test is too slow or blocked, include the command, failure/blocker, and any narrower verification that passed.

## Output

Return JSON matching `harness/schemas/task-result.schema.json`.
