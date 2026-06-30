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
- 사용자 생성은 `user:write:*`, 사용자 수정/삭제는 `user:manage:*`, 사용자 직접 권한 부여/삭제는 `user:grant:*`로 관리합니다.
- `user_permissions`는 부서 권한이 아니라 ADMIN 전용 예외 권한 저장소입니다.
- 부서에 의해 계산된 권한은 사용자 상세 응답에 함께 표시되지만, 사용자 직접 권한 API에서는 조회/수정하지 않습니다.

## 현재 접근 정책

| 접근 조건 | API 목록 |
| --- | --- |
| `isAuthenticated()` | `GET /api/v1/users/me`, `PATCH /api/v1/users/me` |
| `hasRole('ADMIN') or hasAuthority('user:read:*')` | `GET /api/v1/users` |
| `hasRole('ADMIN') or hasAuthority('user:read:*')` | `GET /api/v1/users/{userId}` |
| `hasRole('ADMIN') or hasAuthority('user:read:*') or hasAuthority('user:grant:*')` | `GET /api/v1/users/{userId}/permissions` |
| `hasRole('ADMIN') or hasAuthority('user:write:*')` | `POST /api/v1/users` |
| `hasRole('ADMIN') or hasAuthority('user:manage:*')` | `PATCH /api/v1/users/{userId}`, `DELETE /api/v1/users/{userId}` |
| `hasRole('ADMIN') or hasAuthority('user:grant:*')` | `POST /api/v1/users/{userId}/permissions`, `DELETE /api/v1/users/{userId}/permissions` |

## API별 메모

| API | 의미 | 메모 |
| --- | --- | --- |
| `GET /api/v1/users/me` | 본인 조회 | 타인 조회 권한과 분리된 self access |
| `PATCH /api/v1/users/me` | 본인 수정 | 본인 예외 경로, 역할 변경/권한 변경은 포함하지 않음 |
| `GET /api/v1/users` | 전체 사용자 목록 조회 | 전역 조회 권한 필요 |
| `GET /api/v1/users/{userId}` | 특정 사용자 조회 | 목록 조회와 같은 읽기 권한으로 관리 |
| `POST /api/v1/users` | 사용자 생성 | 전역 생성 권한 필요 |
| `PATCH /api/v1/users/{userId}` | 특정 사용자 수정 | 전역 관리 권한 필요. `role=GUEST`는 교원 해제로 처리 |
| `DELETE /api/v1/users/{userId}` | 특정 사용자 삭제 | 전역 관리 권한 필요 |
| `GET /api/v1/users/{userId}/permissions` | 특정 사용자 직접 권한 조회 | `user_permissions`에 저장된 수동 예외 권한만 조회 |
| `POST /api/v1/users/{userId}/permissions` | 특정 사용자 직접 권한 부여 | `user:grant:*` 권한으로만 허용 |
| `DELETE /api/v1/users/{userId}/permissions` | 특정 사용자 직접 권한 회수 | `user:grant:*` 권한으로만 허용 |

## Permission Code 초안

아래 표는 현재 코드에 맞춘 단순화된 `user` 리소스 기준 초안입니다.

| permission code | 범위 | API 목록 |
| --- | --- | --- |
| `user:read:*` | 전체 사용자 및 특정 사용자 상세 조회 | `GET /api/v1/users`, `GET /api/v1/users/{userId}` |
| `user:write:*` | 사용자 생성 | `POST /api/v1/users` |
| `user:manage:*` | 사용자 수정/삭제 | `PATCH /api/v1/users/{userId}`, `DELETE /api/v1/users/{userId}` |
| `user:grant:*` | 사용자 직접 권한 조회/부여/회수 | `GET /api/v1/users/{userId}/permissions`, `POST /api/v1/users/{userId}/permissions`, `DELETE /api/v1/users/{userId}/permissions` |

## 설계 메모

- `/me` API는 permission code로 관리하지 않고 self access 규칙으로 유지하는 편이 단순합니다.
- 사용자 직접 권한 부여/회수는 `user:grant:*`로 분리합니다.
- `user:grant:*`와 `department:grant:*`는 운영상 ADMIN 전용 예외 권한으로 취급합니다.
- 사용자 상세 응답의 `permissions`에는 직접 권한(`MANUAL`)과 부서 직책 권한(`MEMBER`, `MANAGER`)이 함께 표시될 수 있습니다.
- 사용자 직접 권한 API는 `user_permissions`만 다루며, 부서 직책 권한은 `department_permissions`에서 관리합니다.
- `role=GUEST`로 사용자 수정 시 교원 해제로 처리하여 소속 부서, 배정 분반, 직접 권한을 회수합니다.

## 코드 기준 위치

- `UserAdminController`: `src/main/java/geumjeongyahak/domain/users/v1/controller/UserAdminController.java`
- `UserSelfController`: `src/main/java/geumjeongyahak/domain/users/v1/controller/UserSelfController.java`
- `UserPermissionController`: `src/main/java/geumjeongyahak/domain/users/v1/controller/UserPermissionController.java`
