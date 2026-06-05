# Department Permission

부서 권한은 부서 소속과 사용자의 기본 역할을 조합해 계산합니다.

- 기준 코드: `src/main/java/geumjeongyahak/domain/department`
- 저장 테이블: `department_permissions`
- 응답 출처: `PermissionResponse.source`

## 원칙

- `department_permissions`는 부서 직책별 권한 저장소입니다.
- 일반 부서원 권한은 `role_type=MEMBER`로 저장합니다.
- 부서장 추가 권한은 `role_type=MANAGER`로 저장합니다.
- `user_permissions`는 부서 권한이 아니라 ADMIN이 수동으로 부여하는 사용자 예외 권한입니다.
- 사용자 상세 응답에서는 직접 권한은 `source=MANUAL`, 부서 권한은 `source=MEMBER` 또는 `source=MANAGER`로 표시합니다.

## 적용 규칙

| 사용자 상태 | 적용되는 부서 권한 |
| --- | --- |
| 부서 없음 | 없음 |
| `VOLUNTEER` + 부서 소속 | 해당 부서의 `MEMBER` 권한 |
| `MANAGER` + 부서 소속 | 해당 부서의 `MEMBER` + `MANAGER` 권한 |
| `ADMIN` | 부서 권한 계산 없이 `ROLE_ADMIN`으로 관리자 권한 처리 |
| `GUEST` | 없음 |

## Department API 정책

| API | 권한 요구사항 | 메모 |
| --- | --- | --- |
| `GET /api/v1/departments` | 인증 사용자 | 부서 목록 조회 |
| `GET /api/v1/departments/{id}` | 인증 사용자 | 권한 목록과 소속 사용자 포함 |
| `POST /api/v1/departments` | `ADMIN` 또는 `department:write:*` | permissions 없이 생성 가능 |
| `POST /api/v1/departments` + permissions | `ADMIN` 또는 `department:write:*` + `department:grant:*` | 부서 권한까지 함께 저장 |
| `PUT /api/v1/departments/{id}` | `ADMIN` 또는 `department:manage:*` | permissions 없이 기본 정보 수정 가능 |
| `PUT /api/v1/departments/{id}` + permissions | `ADMIN` 또는 `department:manage:*` + `department:grant:*` | 부서 권한 전체 교체 |
| `DELETE /api/v1/departments/{id}` | `ADMIN` 또는 `department:manage:*` | 소속 사용자가 있으면 삭제 불가 |

## 권한 교체 규칙

`PUT /api/v1/departments/{id}`에서 `permissions` 필드는 부분 수정이 아닙니다.

| 요청 값 | 동작 |
| --- | --- |
| `permissions` 생략 또는 `null` | 기존 부서 권한 유지 |
| `permissions: []` | 기존 부서 권한 전체 삭제 |
| `permissions: [...]` | 기존 부서 권한 전체 삭제 후 요청 목록 저장 |

`roleType`을 생략한 권한은 `MEMBER`로 저장됩니다.

## 초기 부서 권한

`src/main/resources/sql/init_data.sql` 기준 초기 권한은 다음과 같습니다.

| 부서 | MEMBER | MANAGER |
| --- | --- | --- |
| 교무기획부 | `subject:read:*`, `lesson:read:*`, `channel:write:14` | `subject:write:*`, `subject:manage:*`, `lesson:write:*`, `lesson:manage:*`, `event:manage:*`, `lesson-exchange-request:manage:*`, `channel:manage:14` |
| 교육연구부 | `teacher-application:read:*`, `daily-schedule:read:*`, `absence-request:read:*`, `channel:write:15` | `student:write:*`, `student:manage:*`, `teacher-application:manage:*`, `daily-schedule:manage:*`, `absence-request:manage:*`, `channel:manage:15` |
| 생활안전부 | `purchase-request:read:*`, `channel:write:16` | `purchase-request:review:*`, `channel:manage:16` |
| 총무부 | `purchase-request:read:*`, `vendor:read:*`, `channel:write:17` | `purchase-request:review:*`, `purchase-request:manage:*`, `vendor:manage:*`, `channel:manage:17` |
| 홍보부 | `teacher-application:read:*`, `channel:write:18` | `channel:manage:18` |

테스트 초기 데이터는 같은 정책을 사용하되 테스트 채널 ID에 맞춰 권한 target 값이 다릅니다.
