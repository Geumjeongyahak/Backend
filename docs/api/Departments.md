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
부서의 상세 정보를 조회합니다. 세부 권한 정보와 소속 사용자 목록을 포함합니다.

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
        "name": "department:read:*",
        "code": "department:read:*"
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
- **Authorization**: 관리자 권한 필요 (`hasRole('ADMIN')`)
- **Request Body**:
  ```json
  {
    "name": "신설부서",
    "description": "새로운 업무를 담당하는 팀",
    "permissions": [
      { "permissionCode": "post:read:*" }
    ]
  }
  ```
  - `name` (String, required): 부서 이름
  - `description` (String, required): 부서 설명
  - `permissions` (Array, optional): 부서에 부여할 세부 권한 코드 목록
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

### 1.4. 부서 수정
기존 부서의 정보를 수정합니다.

- **URL**: `/api/v1/departments/{id}`
- **Method**: `PUT`
- **Authorization**: 관리자 권한 필요 (`hasRole('ADMIN')`)
- **Path Parameters**:
  - `id` (Long): 부서 ID
- **Request Body**:
  ```json
  {
    "name": "수정된부서명",
    "permissions": [
      { "permissionCode": "post:write:*" }
    ]
  }
  ```
- **Response**: `200 OK` (DepartmentSimpleResponse)
- **Error Response**:
  - `404 Not Found`: 부서를 찾을 수 없음
  - `403 Forbidden`: 권한 없음

### 1.5. 부서 삭제
부서를 삭제합니다.

- **URL**: `/api/v1/departments/{id}`
- **Method**: `DELETE`
- **Authorization**: 관리자 권한 필요 (`hasRole('ADMIN')`)
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
| 부서 생성 | ADMIN 역할 |
| 부서 수정 | ADMIN 역할 |
| 부서 삭제 | ADMIN 역할 |

---

## 참고사항

1. **부서 삭제 제약사항**:
   - 소속된 멤버가 있는 부서는 삭제할 수 없습니다.
   - 삭제하려면 먼저 모든 멤버의 소속을 변경하거나 해제해야 합니다.

2. **사용자 소속 관리**:
   - 사용자의 부서 소속은 User API(`POST /api/v1/users`, `PATCH /api/v1/users/{userId}`)를 통해 관리자가 설정합니다.
   - 사용자는 자신의 부서를 직접 변경할 수 없습니다.

3. **권한 코드 모델**:
   - 부서에 부여된 권한(`permission_code`)은 해당 부서에 속한 모든 사용자에게 자동으로 적용됩니다.
   - 형식: `{resource}:{action}:{target}` (예: `post:read:*`)
