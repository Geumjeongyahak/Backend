# Docs INDEX

금정야학 백엔드(GJLearn API) 문서 전체 색인입니다. **찾는 것이 어느 칸에 있는지부터
정하고 들어갑니다.**

| 무엇을 하려는가 | 어디로 |
|---|---|
| 이 프로젝트가 뭔지 알고 싶다 | [PRD](./prd.md) → [기술 명세서](./tech_spec.md) |
| API를 붙이려 한다 | [API 안내](./api-spec/api_spec.md) → [도메인별 API](./api/README.md) |
| 코드를 고치려 한다 | [개발 컨벤션](./convention/convention.md) → [데이터 모델](./data_model.md) |
| 작업을 시작하려 한다 | [하네스 v2](../harness/v2/2026-08-08/README.md) |
| 배포·인프라를 만지려 한다 | [DEPLOY.md](../DEPLOY.md) → [배포 문서](#6-배포운영) |

---

## 1. 제품 · 설계

| 문서 | 내용 |
|---|---|
| [PRD](./prd.md) | 제품 요구사항과 기능 우선순위 |
| [기술 명세서](./tech_spec.md) | 아키텍처, 기술 스택, 보안·권한 구현 방향 |
| [데이터 모델](./data_model.md) | 엔티티와 관계, 테이블 정의, 역할·권한 테이블 |
| [Channel/Post 인수인계서](./channel_post_handover_2026_05_04.md) | 구현 완료 범위, 남은 작업, E2E 영향 |

## 2. API

**공통 규칙이 먼저입니다.** 응답 구조와 문서 읽는 순서는 안내 문서에 있습니다.

| 문서 | 내용 |
|---|---|
| [API 안내](./api-spec/api_spec.md) | 문서 구조, 공통 응답 규칙, 참조 순서 |
| [도메인별 API](./api/README.md) | 18개 도메인의 엔드포인트·권한·side effect·실패 케이스 |
| [Error Codes](./error_codes.md) | 에러 코드 규칙과 코드표. **에러는 이 문서가 단일 기준** |

도메인별 문서 바로가기 —
[Auth](./api/Auth.md) ·
[Users](./api/Users.md) ·
[Departments](./api/Departments.md) ·
[Classrooms](./api/Classrooms.md) ·
[Students](./api/Students.md) ·
[Subjects](./api/Subjects.md) ·
[TeacherApplications](./api/TeacherApplications.md) ·
[Lessons](./api/Lessons.md) ·
[Events](./api/Events.md) ·
[DailySchedules](./api/DailySchedules.md) ·
[Requests](./api/Requests.md) ·
[PurchaseRequests](./api/PurchaseRequests.md) ·
[Channels](./api/Channels.md) ·
[Posts](./api/Posts.md) ·
[Files](./api/Files.md) ·
[Comments](./api/Comments.md) ·
[SiteContent](./api/SiteContent.md)

프론트 연동 가이드는 [TeacherAssignmentFrontendGuide](./api/TeacherAssignmentFrontendGuide.md).

## 3. 도메인 상세

`docs/api/`가 엔드포인트라면 `docs/Domain/`은 도메인 하나를 통째로 설명합니다.

| 문서 | 내용 |
|---|---|
| [User 도메인](./Domain/User/README.md) | 사용자 도메인 전체 설계 |
| [User API](./Domain/User/API.md) | 사용자 API 상세와 응답 필드 |
| [User 권한](./Domain/User/Permissions.md) | 사용자 권한 정책 |
| [User 시퀀스](./Domain/User/SequenceDiagram.md) | 가입·인증·권한 흐름 |
| [SiteContent 도메인](./Domain/SiteContent/README.md) | 기관 정보 정적 페이지 설계와 작업 순서 |

## 4. 권한

| 문서 | 내용 |
|---|---|
| [Permission 안내](./permission/README.md) | 권한 모델과 접근 정책 정리 기준 |
| [users 권한](./permission/users.md) | 사용자 도메인 접근 정책 |
| [departments 권한](./permission/departments.md) | 부서 도메인 접근 정책 |

역할은 `RoleType` 넷(ADMIN·MANAGER·VOLUNTEER·GUEST)이고, 세밀 권한은
`PermissionCode`(`resource:action:target`)입니다. 요약은
[README](../README.md#권한-체계), 구현 방향은 [기술 명세서](./tech_spec.md)에 있습니다.

## 5. 개발 규약

| 문서 | 내용 |
|---|---|
| [개발 컨벤션](./convention/convention.md) | 커밋·브랜치·코드 스타일·패키지 구조·Issue/PR 규칙 |
| [SQL 규약](./convention/sql.md) | SQL 작성 기준 |
| [PR 템플릿](./convention/pr_template.md) | 사본. **원본은 [`.github/PULL_REQUEST_TEMPLATE.md`](../.github/PULL_REQUEST_TEMPLATE.md)** |
| [Issue 템플릿 - Feature](./convention/issue_feature_template.md) | 사본. 원본은 `.github/ISSUE_TEMPLATE/feature-request.md` |
| [Issue 템플릿 - Fix](./convention/issue_bug_template.md) | 사본. 원본은 `.github/ISSUE_TEMPLATE/bug_report.md` |

## 6. 배포 · 운영

| 문서 | 내용 |
|---|---|
| [DEPLOY.md](../DEPLOY.md) | 배포 절차 전체 |
| [GCP 프로비저닝 스크립트](../scripts/gcp/README.md) | `scripts/gcp/` 단계별 스크립트 |
| [GCE SSH 설정](./deploy/gce-ssh-setup.md) | 배포용 SSH 구성 |
| [GCE PostgreSQL · Flyway](./deployment/gce-postgres-flyway.md) | DB 서버와 마이그레이션 운영 |
| [gcloud 프로비저닝](./deployment/gcloud-provisioning.md) | 인프라 생성 절차 |
| [Tailscale · 관측 배포](./deployment/tailscale-observability-deploy.md) | 사설망과 관측 스택 |
| [모니터링](../infra/monitoring/README.md) | 관측 구성 |
| [E2E 테스트 파이프라인](./e2e-testing-pipeline.md) | 테스트 파이프라인 설계와 운영 메모 |

## 7. 작업 흐름 (하네스)

이슈를 만들고 구현하고 내보내는 절차입니다. 진입점은 `.claude/skills/`에 있습니다.

| 문서 | 내용 |
|---|---|
| [하네스 레지스트리](../harness/README.md) | 버전 목록과 원장 위치 |
| [하네스 v2](../harness/v2/2026-08-08/README.md) | 현행. 워크플로 계약 |
| [ideate-issue](../harness/v2/2026-08-08/workflows/ideate-issue.md) | 무엇을 왜 할지 정한다 → 이슈 |
| [work-an-issue](../harness/v2/2026-08-08/workflows/work-an-issue.md) | 설계 → 구현 → 테스트 → 리뷰 |
| [land-a-pr](../harness/v2/2026-08-08/workflows/land-a-pr.md) | 검증 → PR → 병합 |
| [codex 리뷰](../harness/v2/2026-08-08/codex-review.md) | 다른 눈. 리뷰 프로토콜 |

## 8. 작업 기록

| 위치 | 내용 |
|---|---|
| [`docs/issue/`](./issue/) | 개별 이슈 메모 (v2 이전) |
| `docs/issues/<번호>-<slug>/` | 하네스 v2의 이슈 폴더 (`spec.md` · `plan.md` · `review.md`) |

---

## 문서 원칙

- **단일 기준을 정합니다.** 에러 코드는 [`error_codes.md`](./error_codes.md),
  개발 규약은 [`convention/convention.md`](./convention/convention.md),
  응답·권한·도메인 통신 요약은 [`AGENTS.md`](../AGENTS.md)(=`CLAUDE.md`)입니다.
  같은 사실을 두 곳에 적으면 갱신될 때 한쪽만 낡습니다.
- `api-spec/api_spec.md`는 안내와 공통 규칙만, `api/*.md`는 도메인별 엔드포인트만 다룹니다.
- `convention/` 아래 템플릿 셋은 **사본입니다.** GitHub이 실제로 읽는 것은 `.github/`
  아래이고, 고칠 때는 원본을 먼저 고칩니다.
- 작업 기록 문서는 제품 명세를 대신하지 않습니다.
- **코드에 없는 것을 적지 않습니다.** 설계 중인 것은 «미정»으로 표시하고, 정해진 것과
  구별합니다.
