# v1 Folder Structure

## 원칙

- `harness/versions/`는 설계 히스토리다. v1/v2 문서가 날짜별로 누적된다.
- `harness/prompts`, `harness/schemas`, `harness/tasks`는 runner가 바로 쓰는 active artifact다.
- `harness/runs`는 로컬 실행 결과이며 git에 올리지 않는다.
- `scripts/harness`는 실행 가능한 wrapper/hook이다.

## 구조

```text
harness/
├── versions/
│   ├── README.md
│   └── v1-2026-06-28/
│       ├── README.md
│       ├── folder-structure.md
│       ├── workflow.md
│       ├── prompt-monitoring.md
│       └── prompts/
│           └── issue-branch-commit-pr.md
├── prompts/
│   └── issue-branch-commit-pr.md
├── schemas/
│   └── task-result.schema.json
├── tasks/
│   └── monitoring-sync-docs-task.template.md
└── runs/
    └── <run-id>/
        ├── prompt.txt
        ├── events.jsonl
        ├── result.json
        ├── verify.log
        ├── verify-summary.txt
        ├── diff-summary.md
        └── monitor.jsonl

scripts/harness/
├── run-task.sh
├── monitor-hook.sh
├── verify.sh
└── summarize-diff.sh
```

## Active와 versioned의 관계

- active prompt는 `harness/prompts/issue-branch-commit-pr.md`에 둔다.
- 같은 내용의 v1 snapshot은 `harness/versions/v1-2026-06-28/prompts/`에 보존한다.
- v2에서 prompt recipe가 바뀌면 새 version folder를 만들고 active prompt를 v2로 승격한다.
- runner는 `HARNESS_VERSION` 환경 변수로 versioned prompt를 선택할 수 있어야 한다.

## 저장하지 않는 것

- API keys, OAuth secrets, deploy keys
- 홈랩 운영 서버의 live secret/config dump
- Codex raw output에 secrets가 섞인 artifact
- build output, Gradle cache, local logs
