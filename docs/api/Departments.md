# Department API

부서 관리 및 사용자-부서 참여 관리를 위한 API 명세입니다.

---

## 1. 부서 관리 API

### 1.1. 부서 목록 조회
모든 부서 목록을 조회합니다.

- **URL**: `/api/v1/departments`
- **Method**: `GET`
- **Authorization**: 인증 필요 (`isAuthenticated()`)
- **Query Parameters**: 없음
- **Response**: `200 OK`
  ```json
  {
    "departments": [
      {
        "id": 1,
        "name": "개발팀",
        "description": "소프트웨어 개발을 담당하는 팀"
      },
      {
        "id": 2,
        "name": "재정팀",
        "description": "예산 관리 및 회계를 담당하는 팀"
      }
    ]
  }
  ```

### 1.2. 부서 상세 조회
부서의 상세 정보를 조회합니다. 할당된 역할 정보와 소속 사용자 목록을 포함합니다.

- **URL**: `/api/v1/departments/{id}`
- **Method**: `GET`
- **Authorization**: 인증 필요 (`isAuthenticated()`)
- **Path Parameters**:
  - `id` (Long): 부서 ID
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "name": "개발팀",
    "description": "소프트웨어 개발을 담당하는 팀",
    "assignedRole": {
      "id": 1001,
      "name": "DEPT_FINANCE",
      "description": "재정 부서"
    },
    "users": [
      {
        "id": 1,
        "email": "user@example.com",
        "name": "홍길동",
        "phoneNumber": "010-1234-5678",
        "isActive": true
      }
    ]
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
    "name": "개발팀",
    "description": "소프트웨어 개발을 담당하는 팀"
  }
  ```
  - `name` (String, required): 부서 이름
  - `description` (String, required): 부서 설명
- **Response**: `201 Created`
  ```json
  {
    "id": 1,
    "name": "개발팀",
    "description": "소프트웨어 개발을 담당하는 팀"
  }
  ```
- **Error Response**:
  - `400 Bad Request`: 유효성 검증 실패
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
    "name": "개발팀",
    "description": "소프트웨어 개발 및 유지보수를 담당하는 팀"
  }
  ```
  - `name` (String, optional): 수정할 부서 이름
  - `description` (String, optional): 수정할 부서 설명
  - 참고: 둘 다 optional이지만, 최소한 하나는 제공되어야 합니다.
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "name": "개발팀",
    "description": "소프트웨어 개발 및 유지보수를 담당하는 팀"
  }
  ```
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
  - `400 Bad Request`: 할당된 역할이 있거나 소속 멤버가 있어 삭제할 수 없음
    - `DELETE_DEPARTMENT_WITH_ROLE`: 역할이 할당된 부서는 삭제할 수 없습니다.
    - `DELETE_DEPARTMENT_WITH_MEMBER`: 멤버가 있는 부서는 삭제할 수 없습니다.
  - `403 Forbidden`: 권한 없음

---

## 2. 사용자-부서 참여 API

### 2.1. 부서 참여
사용자가 특정 부서에 참여합니다.

- **URL**: `/api/v1/users/{userId}/departments`
- **Method**: `POST`
- **Authorization**: 관리자 또는 본인만 가능 (`hasRole('ADMIN') or #userId == authentication.principal.userId`)
- **Path Parameters**:
  - `userId` (Long): 사용자 ID
- **Request Body**:
  ```json
  {
    "departmentId": 1
  }
  ```
  - `departmentId` (Long, required): 참여할 부서 ID
- **Response**: `201 Created`
- **Error Response**:
  - `404 Not Found`: 사용자 또는 부서를 찾을 수 없음
  - `400 Bad Request`: 이미 참여 중인 부서
  - `403 Forbidden`: 권한 없음

### 2.2. 부서 탈퇴
사용자가 부서에서 탈퇴합니다.

- **URL**: `/api/v1/users/{userId}/departments/{departmentId}`
- **Method**: `DELETE`
- **Authorization**: 관리자 또는 본인만 가능 (`hasRole('ADMIN') or #userId == authentication.principal.userId`)
- **Path Parameters**:
  - `userId` (Long): 사용자 ID
  - `departmentId` (Long): 부서 ID
- **Response**: `204 No Content`
- **Error Response**:
  - `404 Not Found`: 사용자, 부서 또는 참여 정보를 찾을 수 없음
  - `403 Forbidden`: 권한 없음

### 2.3. 사용자 소속 부서 목록 조회
특정 사용자가 소속된 부서 목록을 조회합니다.

- **URL**: `/api/v1/users/{userId}/departments`
- **Method**: `GET`
- **Authorization**: 인증 필요 (`isAuthenticated()`)
- **Path Parameters**:
  - `userId` (Long): 사용자 ID
- **Response**: `200 OK`
  ```json
  {
    "departments": [
      {
        "id": 1,
        "name": "개발팀",
        "description": "소프트웨어 개발을 담당하는 팀"
      },
      {
        "id": 2,
        "name": "재정팀",
        "description": "예산 관리 및 회계를 담당하는 팀"
      }
    ]
  }
  ```
- **Error Response**:
  - `404 Not Found`: 사용자를 찾을 수 없음

### 2.4. 본인 소속 부서 목록 조회
현재 로그인한 사용자가 소속된 부서 목록을 조회합니다.

- **URL**: `/api/v1/users/me/departments`
- **Method**: `GET`
- **Authorization**: 인증 필요 (`isAuthenticated()`)
- **Response**: `200 OK`
  ```json
  {
    "departments": [
      {
        "id": 1,
        "name": "개발팀",
        "description": "소프트웨어 개발을 담당하는 팀"
      }
    ]
  }
  ```

---

## 도메인 이벤트

부서 관련 작업 시 다음 이벤트가 발행됩니다:

### JoinDepartmentEvent
사용자가 부서에 참여할 때 발행됩니다.
- **발행 시점**: 사용자 부서 참여 성공 후
- **이벤트 데이터**:
  - `userId`: 참여한 사용자 ID
  - `departmentId`: 참여한 부서 ID
- **처리**: `DepartmentRoleHandler`에서 부서에 할당된 역할을 사용자에게 자동 부여

### LeaveDepartmentEvent
사용자가 부서에서 탈퇴할 때 발행됩니다.
- **발행 시점**: 사용자 부서 탈퇴 성공 후
- **이벤트 데이터**:
  - `userId`: 탈퇴한 사용자 ID
  - `departmentId`: 탈퇴한 부서 ID
- **처리**: `DepartmentRoleHandler`에서 부서에 할당된 역할을 사용자에게서 자동 제거

---

## 권한 정책

| API | 권한 요구사항 |
|-----|--------------|
| 부서 목록 조회 | 인증된 사용자 |
| 부서 상세 조회 | 인증된 사용자 |
| 부서 생성 | ADMIN 역할 |
| 부서 수정 | ADMIN 역할 |
| 부서 삭제 | ADMIN 역할 |
| 부서 참여 | ADMIN 또는 본인 |
| 부서 탈퇴 | ADMIN 또는 본인 |
| 사용자 부서 목록 조회 | 인증된 사용자 |
| 본인 부서 목록 조회 | 인증된 사용자 |

---

## 참고사항

1. **부서 삭제 제약사항**:
   - 할당된 역할(`roleId`)이 있는 부서는 삭제할 수 없습니다.
   - 소속된 멤버가 있는 부서는 삭제할 수 없습니다.
   - 삭제하려면 먼저 역할 할당을 해제하고 모든 멤버를 탈퇴시켜야 합니다.

2. **역할 자동 관리**:
   - 부서에 역할이 할당되어 있으면, 사용자가 부서에 참여할 때 해당 역할이 자동으로 부여됩니다.
   - 부서에서 탈퇴하면 해당 역할이 자동으로 제거됩니다.

3. **Response 구조**:
   - 모든 응답은 실제 데이터 객체로 반환됩니다 (ApiResponse wrapper 없음).
   - 에러 응답은 공통 예외 처리기에서 ApiResponse 형식으로 변환됩니다.
