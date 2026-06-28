# v1 Prompt and Result Monitoring

## 목표

프롬프트와 결과를 단순 로그가 아니라 재현 가능한 run artifact로 다룬다. Codex/Hermes의 자기보고는 참고 정보이고, 하네스는 prompt hash, result hash, 검증 로그, diff summary를 별도 evidence로 보존한다.

## 관측 대상

| 대상 | 파일 | 이유 |
| --- | --- | --- |
| rendered prompt | `prompt.txt` | 동일 task가 어떤 지시로 실행됐는지 재현 |
| Codex events | `events.jsonl` | tool/event 흐름 추적 |
| structured result | `result.json` | schema 기반 자동 집계 |
| verification log | `verify.log` | self-report와 별도 검증 evidence |
| verification summary | `verify-summary.txt` | PR 본문에 붙일 압축 결과 |
| diff summary | `diff-summary.md` | reviewer handoff |
| monitor hook ledger | `monitor.jsonl` | prompt/result file hash와 timestamp |

## Hook contract

`monitor-hook.sh` 입력:

```text
scripts/harness/monitor-hook.sh <run-id> <event> [file] [status]
```

출력은 `monitor.jsonl` append-only JSONL이다.

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
- `harness/runs/`는 git ignore한다.
- PR에는 artifact 경로와 요약만 남기고 raw secret 가능성이 있는 로그는 붙이지 않는다.
