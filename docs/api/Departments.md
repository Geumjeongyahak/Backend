# Department API

부서 관리 및 부서 상세 조회를 위한 API 명세입니다.

---

## 1. 부서 관리 API

### 1.1. 부서 목록 조회
모든 부서 목록을 조회합니다.

- **URL**: `/api/v1/departments`
- **Method**: `GET`
- **Authorization**: 인증 필요 (`isAuthenticated()`)
- **Query Parameters**: 없음
- **Response**: `200 OK` (DepartmentListResponse)
  ```json
  {
    "departments": [
      {
        "id": 1,
        "name": "교무기획부",
        "description": "기관의 교육 운영 계획 수립..."
      },
      {
        "id": 2,
        "name": "교육연구부",
        "description": "교육 프로그램 연구..."
      }
    ]
  }
  ```

### 1.2. 부서 상세 조회
부서의 상세 정보를 조회합니다. 직책별 권한 정보와 소속 사용자 목록을 포함합니다.

- **URL**: `/api/v1/departments/{id}`
- **Method**: `GET`
- **Authorization**: 인증 필요 (`isAuthenticated()`)
- **Path Parameters**:
  - `id` (Long): 부서 ID
- **Response**: `200 OK` (DepartmentDetailResponse)
  ```json
  {
    "id": 1,
    "name": "교무기획부",
    "description": "기관의 교육 운영 계획 수립...",
    "permissions": [
      {
        "name": "channel:write:14",
        "code": "channel:write:14",
        "source": "MEMBER"
      },
      {
        "name": "channel:manage:14",
        "code": "channel:manage:14",
        "source": "MANAGER"
      }
    ],
    "users": [
      {
        "id": 1,
        "name": "관리자",
        "email": "admin@test.com",
        "role": "ADMIN",
        "departmentId": 1
      }
    ],
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-02T15:30:00"
  }
  ```
- **Error Response**:
  - `404 Not Found`: 부서를 찾을 수 없음

### 1.3. 부서 생성
새로운 부서를 생성합니다.

- **URL**: `/api/v1/departments`
- **Method**: `POST`
- **Authorization**:
  - `ADMIN`
  - 또는 `department:write:*`
  - 단, `permissions`를 함께 보내려면 `department:grant:*`도 필요
- **Request Body**:
  ```json
  {
    "name": "신설부서",
    "description": "새로운 업무를 담당하는 팀",
    "permissions": [
      {
        "roleType": "MEMBER",
        "permissionCode": "channel:write:19"
      },
      {
        "roleType": "MANAGER",
        "permissionCode": "channel:manage:19"
      }
    ]
  }
  ```
  - `name` (String, required): 부서 이름
  - `description` (String, required): 부서 설명
  - `permissions` (Array, optional): 부서 직책별 권한 목록
  - `permissions[].roleType` (String, optional): `MEMBER` 또는 `MANAGER`. 생략하면 `MEMBER`
  - `permissions[].permissionCode` (String, required): 권한 코드
- **Response**: `201 Created` (DepartmentSimpleResponse)
  ```json
  {
    "id": 7,
    "name": "신설부서",
    "description": "새로운 업무를 담당하는 팀"
  }
  ```
- **Error Response**:
  - `400 Bad Request`: 유효성 검증 실패 (잘못된 권한 코드 등)
  - `403 Forbidden`: 권한 없음
- **Side Effects**:
  - `department_permissions`에 직책별 권한이 저장될 수 있습니다.
  - 부서 생성 이벤트 이후 부서 채널이 생성되면 해당 채널의 `channel:read:{channelId}`, `channel:write:{channelId}` 권한이 `MEMBER` 권한으로 추가될 수 있습니다.

### 1.4. 부서 수정
기존 부서의 정보를 수정합니다.

- **URL**: `/api/v1/departments/{id}`
- **Method**: `PUT`
- **Authorization**:
  - `ADMIN`
  - 또는 `department:manage:*`
  - 단, `permissions`를 함께 보내려면 `department:grant:*`도 필요
- **Path Parameters**:
  - `id` (Long): 부서 ID
- **Request Body**:
  ```json
  {
    "name": "수정된부서명",
    "permissions": [
      {
        "roleType": "MEMBER",
        "permissionCode": "channel:write:19"
      },
      {
        "roleType": "MANAGER",
        "permissionCode": "channel:manage:19"
      }
    ]
  }
  ```
- **Request Notes**:
  - `name`, `description`은 전달한 값만 변경합니다.
  - `permissions`가 `null`이면 기존 부서 권한을 유지합니다.
  - `permissions`를 보내면 부분 수정이 아니라 해당 부서 권한 전체 교체입니다.
  - `permissions`를 빈 배열로 보내면 해당 부서 권한이 모두 삭제됩니다.
- **Response**: `200 OK` (DepartmentSimpleResponse)
- **Error Response**:
  - `404 Not Found`: 부서를 찾을 수 없음
  - `403 Forbidden`: 권한 없음
- **Side Effects**:
  - `permissions`를 보낸 경우 `department_permissions`의 해당 부서 권한이 전체 교체됩니다.

### 1.5. 부서 삭제
부서를 삭제합니다.

- **URL**: `/api/v1/departments/{id}`
- **Method**: `DELETE`
- **Authorization**: `ADMIN` 또는 `department:manage:*`
- **Path Parameters**:
  - `id` (Long): 부서 ID
- **Response**: `204 No Content`
- **Error Response**:
  - `404 Not Found`: 부서를 찾을 수 없음
  - `400 Bad Request`: 소속 멤버가 있어 삭제할 수 없음
  - `403 Forbidden`: 권한 없음

---

## 권한 정책

| API | 권한 요구사항 |
|-----|--------------|
| 부서 목록 조회 | 인증된 사용자 |
| 부서 상세 조회 | 인증된 사용자 |
| 부서 생성 | `ADMIN` 또는 `department:write:*` |
| 부서 생성 + 권한 저장 | `ADMIN` 또는 `department:write:*` + `department:grant:*` |
| 부서 수정 | `ADMIN` 또는 `department:manage:*` |
| 부서 수정 + 권한 전체 교체 | `ADMIN` 또는 `department:manage:*` + `department:grant:*` |
| 부서 삭제 | `ADMIN` 또는 `department:manage:*` |

---

## 참고사항

1. **부서 삭제 제약사항**:
   - 소속된 멤버가 있는 부서는 삭제할 수 없습니다.
   - 삭제하려면 먼저 모든 멤버의 소속을 변경하거나 해제해야 합니다.

2. **사용자 소속 관리**:
   - 사용자의 부서 소속은 User API(`POST /api/v1/users`, `PATCH /api/v1/users/{userId}`)를 통해 관리자가 설정합니다.
   - 사용자는 자신의 부서를 직접 변경할 수 없습니다.

3. **부서 직책별 권한 모델**:
   - 부서 권한은 `department_permissions`에 `department_id`, `role_type`, `permission_code`로 저장됩니다.
   - `role_type=MEMBER` 권한은 해당 부서 소속 `VOLUNTEER`, `MANAGER`에게 적용됩니다.
   - `role_type=MANAGER` 권한은 해당 부서 소속 `MANAGER`에게 추가로 적용됩니다.
   - 형식: `{resource}:{action}:{target}` (예: `channel:write:14`)

4. **사용자 직접 권한과의 구분**:
   - `user_permissions`는 ADMIN이 수동으로 부여하는 사용자 예외 권한입니다.
   - 사용자 상세 응답에서는 직접 권한은 `source=MANUAL`, 부서 권한은 `source=MEMBER` 또는 `source=MANAGER`로 표시됩니다.
