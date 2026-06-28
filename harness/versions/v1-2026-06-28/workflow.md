# v1 Workflow

## State machine

```text
intake
  -> issue.created
  -> branch.created
  -> task.packet.ready
  -> prompt.rendered
  -> codex.finished
  -> verify.finished
  -> diff.summarized
  -> commit.created
  -> pr.opened
  -> review/ci.checked
```

## 1. intake

입력은 사용자 요청, LLM Wiki 맥락, 현재 repo diff다. Hermes는 먼저 관련 wiki page와 repo 상태를 확인한다.

필수 evidence:

- `git status --short --branch`
- 관련 wiki/doc 파일명
- 기존 diff stat

## 2. issue.created

GitHub issue에는 다음을 적는다.

- 배경
- 상세 요구사항
- 수용 기준
- 범위 밖
- 참고 wiki/repo 문서

## 3. branch.created

브랜치 형식:

```text
feature/<issue-number>-<short-topic>
```

dirty tree가 있으면 그 상태를 명시하고, 기존 변경과 새 하네스 변경을 같은 PR에 묶는 이유를 커밋 본문에 쓴다.

## 4. task.packet.ready

`harness/tasks/*.template.md`를 복사해 issue별 task를 만든다. v1 task packet 필수 섹션:

- Goal
- Issue
- Read first
- Modify
- Constraints
- Acceptance criteria
- Verification
- Output

## 5. prompt.rendered

`run-task.sh`가 base prompt + task packet을 `harness/runs/<run-id>/prompt.txt`로 렌더링한다.

Hook 기록:

```json
{"event":"prompt.rendered","file":"prompt.txt","sha256":"...","size_bytes":1234}
```

## 6. codex.finished

Codex turn은 `codex exec --json --output-schema`로 실행한다. 결과는 다음으로 분리한다.

- `events.jsonl`: event stream
- `result.json`: schema-constrained final result

## 7. verify.finished

`verify.sh`가 Codex 보고와 별도로 실제 검증을 수행한다.

v1 기본 검증:

- `git diff --check`
- `bash -n scripts/**/*.sh`
- `python json.load(harness/schemas/*.json)`
- `./gradlew test`

## 8. diff.summarized

`summarize-diff.sh`가 PR handoff용 요약을 만든다.

포함 항목:

- branch/status
- changed files
- diff stat
- recent commits

## 9. commit.created

커밋은 conventional commit을 사용한다. 본문에는 다음을 포함한다.

- 변경 이유
- 묶은 범위
- 검증 명령과 결과
- 남은 위험

## 10. pr.opened

PR 본문 필수 항목:

- Closes #issue
- Harness version: `v1-2026-06-28`
- 변경 요약
- 검증 evidence
- blocker/follow-up
- 리뷰 포인트
