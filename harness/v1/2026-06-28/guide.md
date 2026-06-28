# v1 Guide — Instructions, Guardrails, Team, Prompt Capture

## 리서치 기반 원칙

- Anthropic Claude Code hooks 문서는 도구 실행 전후에 deterministic command를 끼워 넣는 구조를 강조한다. 하네스에서는 LLM 판단보다 hook/runner의 파일 hash, exit code, 검증 로그를 신뢰한다.
- OpenAI Codex CLI 문서는 CLI 기반 실행, streaming/event, prompt/context 관리, sandbox/approval 같은 실행 경계를 제공한다. 하네스에서는 Codex를 최종 권한자가 아니라 bounded diff producer로 둔다.
- GitHub Issues/PR/Actions artifact 문서는 작업 추적과 artifact 공유를 분리한다. 하네스에서는 issue/PR은 사람이 보는 lifecycle이고, `harness/runs/`는 로컬 evidence다.
- LLM Wiki의 [[SW Maestro Harness Research]]와 [[하니스 깎기 심층 조사 보고서]]는 제조 recipe처럼 prompt, schema, runner, policy, verification을 함께 버전 관리하라고 정리한다.

## 최소 루프

```text
issue
  -> branch
  -> task packet
  -> run-task hook
  -> verify
  -> commit
  -> PR
```

v1은 이 루프 밖의 복잡도를 넣지 않는다. DB, dashboard, worktree pool, App Server는 v2 후보로 둔다.

## 지침 작성법

좋은 지침은 길지 않고, 실행자가 헷갈릴 선택지만 줄인다.

| 지침 | 작성 방식 | 예시 |
| --- | --- | --- |
| Goal | 한 문장 결과 | `LOG_FILE_LEVEL을 env로 노출한다.` |
| Read first | 반드시 읽을 파일 | `README.md`, `logback-spring.xml` |
| Modify | 허용 파일 범위 | `scripts/gcp/**`, `HARNESS.md` |
| Constraints | 금지 행동 | secrets, deploy, push, broad refactor 금지 |
| Acceptance | 관측 가능한 완료 조건 | `bash -n`, `./gradlew test` 통과 |
| Output | 기계 판독 결과 | `task-result.schema.json` |

금지:

- “알아서 개선” 같은 넓은 지시.
- 파일 경로 없는 요구사항.
- 검증 없는 완료 기준.
- “필요하면 배포” 같은 숨은 side effect.

## 가드레일

### Scope guard

- task packet의 `Modify` 밖은 수정하지 않는다.
- infra/deploy/secrets는 issue가 명시하지 않으면 읽기 중심으로 둔다.
- generated/build/log/run artifact는 commit하지 않는다.

### Secret guard

- `.env`, `*.env`, deploy key, OAuth secret, token은 prompt와 artifact에 넣지 않는다.
- 입력 수집은 기본 `metadata`만 남긴다.
- 원문 입력을 보존해야 할 때만 `HARNESS_CAPTURE_INPUT=full`을 쓴다.

### Command guard

자동 허용 가능한 명령:

```text
git status
git diff
git diff --check
bash -n scripts/**/*.sh
python/json schema parse
./gradlew test
```

사람 승인이 필요한 명령:

```text
git push
gh pr create
gcloud/ssh deploy
sudo
rm -rf
git reset --hard
secret 출력 가능 command
```

### Verification guard

- Codex final JSON은 참고 자료다.
- 완료 판정은 runner가 다시 실행한 `scripts/harness/verify.sh` 결과가 우선한다.
- 실패하면 `blocked` 또는 `partial`로 남기고 PR에 failure evidence를 적는다.

## 하네스 커스텀 구현 팀

| 역할 | 책임 | 산출물 |
| --- | --- | --- |
| Harness Owner | v1/v2 구조, workflow, guardrail 결정 | `harness/vN/<date>/README.md`, `workflow.md` |
| Prompt Librarian | task template과 prompt recipe 관리 | `harness/prompts/**`, `harness/tasks/**` |
| Runner Engineer | shell hook, result schema, verification wrapper 구현 | `scripts/harness/**`, `harness/schemas/**` |
| Verifier | self-report와 실제 검증 차이 확인 | `verify.log`, `verify-summary.txt` |
| Reviewer | PR risk, rollback, scope violation 확인 | PR review notes |
| Operator | GitHub/GCE/Tailscale 같은 외부 side effect 승인 | issue/PR comment, deploy log |

소규모 팀에서는 한 사람이 여러 역할을 겸해도 된다. 단, 역할 이름은 PR 본문이나 review checklist에서 분리해 생각한다.

## 사용자 프롬프트 입력 수집

기본은 안전한 metadata 수집이다.

```bash
scripts/harness/run-task.sh --dry-run harness/tasks/monitoring-sync-docs-task.template.md
```

원 입력을 artifact로 복사해 프롬프트 품질을 비교하고 싶을 때:

```bash
HARNESS_CAPTURE_INPUT=full scripts/harness/run-task.sh --dry-run harness/tasks/monitoring-sync-docs-task.template.md
```

민감 입력이 있을 때:

```bash
HARNESS_CAPTURE_INPUT=off scripts/harness/run-task.sh --dry-run harness/tasks/monitoring-sync-docs-task.template.md
```

수집 결과는 `harness/runs/<run-id>/monitor.jsonl`에 남는다.

```json
{"event":"task.input","status":"metadata-only","sha256":"..."}
{"event":"prompt.rendered","status":"ok","sha256":"..."}
```

## 사용 팁

- task packet은 “짧게, 경로 중심, 검증 중심”으로 쓴다.
- prompt recipe는 active prompt와 v1 snapshot을 동시에 유지한다.
- v2가 필요해지는 신호는 artifact DB, worktree isolation, GitHub Action, App Server, metric report가 필요해질 때다.
- PR에는 하네스 버전과 날짜를 쓴다: `v1 (2026-06-28)`.
- hook artifact는 리뷰 증거이지 배포 산출물이 아니다.
