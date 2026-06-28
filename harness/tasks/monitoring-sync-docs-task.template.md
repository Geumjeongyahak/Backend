# Task: 모니터링/동기화/문서화 변경

## Goal

금정야학 백엔드의 모니터링, 동기화, 배포 문서 변경을 작은 PR 단위로 구현하고 검증한다.

## Issue

- GitHub issue: #<issue-number>

## Read first

- `AGENTS.md`
- `HARNESS.md`
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
