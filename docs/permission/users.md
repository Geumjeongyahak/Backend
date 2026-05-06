# User Permission

`User` 도메인의 권한 문서입니다.

- 기준 코드: `src/main/java/geumjeongyahak/domain/users/v1/controller`
- 문서 목적: 현재 접근 정책을 먼저 고정하고, 이후 permission code 설계를 덧붙일 수 있게 기준을 만드는 것

## 원칙

- 본인 조회/수정은 `/api/v1/users/me` 경로로 분리합니다.
- `/me` 경로는 권한 코드 없이 `isAuthenticated()`만 요구합니다.
- 타인 대상 API는 `role` 우선, `authority` 후순위로 확인합니다.
- 관리자(`ROLE_ADMIN`)는 모든 사용자 관리 API에 우선 접근할 수 있습니다.
- 사용자 읽기는 `user:read:*` 하나로 관리합니다.
- 사용자 생성/수정/삭제와 사용자 권한 부여/삭제는 `user:manage:*` 하나로 관리합니다.

## 현재 접근 정책

| 접근 조건 | API 목록 |
| --- | --- |
| `isAuthenticated()` | `GET /api/v1/users/me`, `PATCH /api/v1/users/me` |
| `hasRole('ADMIN') or hasAuthority('user:read:*')` | `GET /api/v1/users` |
| `hasRole('ADMIN') or hasAuthority('user:read:*')` | `GET /api/v1/users/{userId}`, `GET /api/v1/users/{userId}/permissions` |
| `hasRole('ADMIN') or hasAuthority('user:manage:*')` | `POST /api/v1/users`, `PATCH /api/v1/users/{userId}`, `DELETE /api/v1/users/{userId}`, `POST /api/v1/users/{userId}/permissions`, `DELETE /api/v1/users/{userId}/permissions` |

## API별 메모

| API | 의미 | 메모 |
| --- | --- | --- |
| `GET /api/v1/users/me` | 본인 조회 | 타인 조회 권한과 분리된 self access |
| `PATCH /api/v1/users/me` | 본인 수정 | 본인 예외 경로, 역할 변경/권한 변경은 포함하지 않음 |
| `GET /api/v1/users` | 전체 사용자 목록 조회 | 전역 조회 권한 필요 |
| `GET /api/v1/users/{userId}` | 특정 사용자 조회 | 목록 조회와 같은 읽기 권한으로 관리 |
| `POST /api/v1/users` | 사용자 생성 | 전역 관리 권한 필요 |
| `PATCH /api/v1/users/{userId}` | 특정 사용자 수정 | 전역 관리 권한 필요 |
| `DELETE /api/v1/users/{userId}` | 특정 사용자 삭제 | 전역 관리 권한 필요 |
| `GET /api/v1/users/{userId}/permissions` | 특정 사용자 권한 조회 | 사용자 읽기 권한과 같은 축으로 관리 |
| `POST /api/v1/users/{userId}/permissions` | 특정 사용자 권한 부여 | 전역 관리 권한으로만 허용 |
| `DELETE /api/v1/users/{userId}/permissions` | 특정 사용자 권한 회수 | 전역 관리 권한으로만 허용 |

## Permission Code 초안

아래 표는 현재 코드에 맞춘 단순화된 `user` 리소스 기준 초안입니다.

| permission code | 범위 | API 목록 |
| --- | --- | --- |
| `user:read:*` | 전체 사용자 조회 | `GET /api/v1/users` |
| `user:read:*` | 특정 사용자 상세/권한 조회 포함 | `GET /api/v1/users/{userId}`, `GET /api/v1/users/{userId}/permissions` |
| `user:manage:*` | 사용자 생성/수정/삭제 및 권한 부여/삭제 | `POST /api/v1/users`, `PATCH /api/v1/users/{userId}`, `DELETE /api/v1/users/{userId}`, `POST /api/v1/users/{userId}/permissions`, `DELETE /api/v1/users/{userId}/permissions` |

## 설계 메모

- `/me` API는 permission code로 관리하지 않고 self access 규칙으로 유지하는 편이 단순합니다.
- 읽기와 관리만 나누고, 그보다 더 세부적인 action 분리는 하지 않습니다.
- `permissions` 하위 API도 별도 리소스로 쪼개지 않고 `user:manage:*`에 포함합니다.
- 권한 수가 과도하게 늘어나는 것을 피하기 위해 사용자 도메인은 최소 권한 집합만 유지합니다.

## 코드 기준 위치

- `UserAdminController`: `src/main/java/geumjeongyahak/domain/users/v1/controller/UserAdminController.java`
- `UserSelfController`: `src/main/java/geumjeongyahak/domain/users/v1/controller/UserSelfController.java`
- `UserPermissionController`: `src/main/java/geumjeongyahak/domain/users/v1/controller/UserPermissionController.java`
