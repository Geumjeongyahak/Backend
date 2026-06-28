# v1 Prompt and Result Monitoring

## 목표

프롬프트와 결과를 단순 로그가 아니라 재현 가능한 run artifact로 다룬다. Codex/Hermes의 자기보고는 참고 정보이고, 하네스는 input hash, prompt hash, result hash, 검증 로그, diff summary를 별도 evidence로 보존한다.

## 관측 대상

| 대상 | 파일 | 이유 |
| --- | --- | --- |
| user/task input | `user-input.md` 또는 task file metadata | 사용자가 준 원 입력 또는 정제 task packet 추적 |
| rendered prompt | `prompt.txt` | 동일 task가 어떤 지시로 실행됐는지 재현 |
| Codex events | `events.jsonl` | tool/event 흐름 추적 |
| structured result | `result.json` | schema 기반 자동 집계 |
| verification log | `verify.log` | self-report와 별도 검증 evidence |
| verification summary | `verify-summary.txt` | PR 본문에 붙일 압축 결과 |
| diff summary | `diff-summary.md` | reviewer handoff |
| monitor hook ledger | `monitor.jsonl` | input/prompt/result file hash와 timestamp |

## Hook contract

`monitor-hook.sh` 입력:

```text
scripts/harness/monitor-hook.sh <run-id> <event> [file] [status]
```

출력은 `monitor.jsonl` append-only JSONL이다.

v1 hook event:

- `task.input`: `HARNESS_CAPTURE_INPUT` 설정에 따라 task file metadata 또는 `user-input.md` copy hash를 기록한다.
- `prompt.rendered`: base prompt와 task packet을 합친 실행 prompt hash를 기록한다.
- `codex.finished`: Codex result file hash와 exit status를 기록한다.
- `verify.finished`: runner verification summary hash와 exit status를 기록한다.
- `diff.summarized`: reviewer handoff summary hash를 기록한다.

```json
{
  "timestamp": "2026-06-28T00:00:00+00:00",
  "run_id": "20260628T000000Z",
  "event": "prompt.rendered",
  "status": "ok",
  "file": "harness/runs/.../prompt.txt",
  "size_bytes": 1234,
  "sha256": "..."
}
```

## 사용자 입력 수집 모드

| Mode | Command | 저장 내용 | 사용 시점 |
| --- | --- | --- | --- |
| `metadata` | default | task file path, size, sha256 | 일반 작업 기본값 |
| `full` | `HARNESS_CAPTURE_INPUT=full ...` | `user-input.md` copy + hash | 프롬프트 비교/리뷰가 필요할 때 |
| `off` | `HARNESS_CAPTURE_INPUT=off ...` | capture-off event only | 민감한 입력이 섞였을 때 |

원 사용자 대화 전체를 자동 수집하지 않는다. 하네스가 수집하는 입력은 task packet 또는 사용자가 명시적으로 파일화한 prompt다. 이 원칙이 secrets 누출을 막는다.

## Metric 후보

v1은 파일 artifact만 남기고 metric DB는 만들지 않는다. v2에서 아래를 집계한다.

- schema pass rate
- hallucinated verification count
- unintended file modification count
- `./gradlew test` pass/fail
- shell syntax failure count
- prompt token/size trend
- retry count
- permission/approval blocker count

## Safety

- artifact에는 secrets를 저장하지 않는다.
- `.env`, `*.env`, deploy key, OAuth secret은 prompt에도 포함하지 않는다.
- `HARNESS_CAPTURE_INPUT=metadata`가 기본값이다. 원 사용자 입력을 복사해야 할 때만 `HARNESS_CAPTURE_INPUT=full`을 쓴다.
- 민감한 입력은 `HARNESS_CAPTURE_INPUT=off`로 실행한다.
- `harness/runs/`는 git ignore한다.
- PR에는 artifact 경로와 요약만 남기고 raw secret 가능성이 있는 로그는 붙이지 않는다.
