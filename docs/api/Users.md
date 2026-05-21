# User API

사용자 정보 조회, 생성, 수정, 삭제 및 부서/권한 관리를 위한 API 명세입니다.

---

## 1. 사용자 관리 API

### 1.1. 사용자 목록 조회
전체 사용자 목록을 페이지네이션 및 검색 조건에 따라 조회합니다.

- **URL**: `/api/v1/users`
- **Method**: `GET`
- **Authorization**: 관리자 권한 필요 (`hasRole('ADMIN')`)
- **Query Parameters**:
    - `page` (optional): 페이지 번호 (0-based)
    - `size` (optional): 페이지 크기
    - `role` (optional): 역할 필터 (`ADMIN`, `MANAGER`, `VOLUNTEER`, `GUEST`)
    - `nickname` (optional): 닉네임 부분 검색
    - `name` (optional): 이름 부분 검색
    - `currentTeacher` (optional): `true` 설정 시 현재 활동 기간 내의 교원만 조회
- **Response**: `200 OK` (PaginationResponse<UserSimpleResponse>)
  ```json
  {
    "content": [
      {
        "id": 1,
        "name": "홍길동",
        "nickname": "까치",
        "email": "user@example.com",
        "phoneNumber": "010-1234-5678",
        "role": "VOLUNTEER",
        "departmentId": 2
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
  ```

### 1.2. 사용자 상세 조회
특정 사용자의 상세 정보를 조회합니다. 세부 권한 목록을 포함합니다.

- **URL**: `/api/v1/users/{userId}`
- **Method**: `GET`
- **Authorization**: 관리자 권한 필요 (`hasRole('ADMIN')`)
- **Path Parameters**:
  - `userId` (Long): 사용자 ID
- **Response**: `200 OK` (UserDetailResponse)
  ```json
  {
    "id": 1,
    "name": "홍길동",
    "nickname": "까치",
    "email": "user@example.com",
    "phoneNumber": "010-1234-5678",
    "role": "VOLUNTEER",
    "departmentId": 2,
    "permissions": [
      { "name": "lesson:write:1", "code": "lesson:write:1" }
    ],
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-02T15:30:00"
  }
  ```

### 1.3. 사용자 본인 조회
현재 로그인한 사용자의 정보를 조회합니다.

- **URL**: `/api/v1/users/me`
- **Method**: `GET`
- **Authorization**: 인증 필요 (`isAuthenticated()`)
- **Response**: `200 OK` (UserDetailResponse)

### 1.4. 사용자 생성
새로운 사용자 계정을 생성합니다.

- **URL**: `/api/v1/users`
- **Method**: `POST`
- **Authorization**: 관리자 권한 필요 (`hasRole('ADMIN')`)
- **Request Body**: (CreateUserRequest)
  ```json
  {
    "email": "newuser@test.com",
    "nickname": "새닉네임",
    "name": "신규사용자",
    "password": "password123!",
    "phoneNumber": "010-1111-2222",
    "role": "VOLUNTEER",
    "departmentId": 1
  }
  ```
- **Response**: `201 Created` (UserDetailResponse)

### 1.5. 사용자 수정 (관리자용)
관리자가 사용자의 정보를 수정합니다. 역할 및 부서 변경이 가능합니다.

- **URL**: `/api/v1/users/{userId}`
- **Method**: `PATCH`
- **Authorization**: 관리자 권한 필요 (`hasRole('ADMIN')`)
- **Request Body**: (UpdateUserRequest)
  ```json
  {
    "name": "수정된이름",
    "role": "MANAGER",
    "departmentId": 3
  }
  ```
- **Response**: `200 OK` (UserDetailResponse)

### 1.6. 사용자 본인 수정
사용자가 자신의 정보를 직접 수정합니다. 역할 및 부서 정보는 수정할 수 없습니다.

- **URL**: `/api/v1/users/me`
- **Method**: `PATCH`
- **Authorization**: 인증 필요 (`isAuthenticated()`)
- **Request Body**: (UpdateSelfRequest)
  ```json
  {
    "nickname": "변경한닉네임",
    "password": "newpassword123!"
  }
  ```
- **Response**: `200 OK` (UserDetailResponse)

### 1.7. 사용자 삭제
사용자 계정을 삭제합니다.

- **URL**: `/api/v1/users/{userId}`
- **Method**: `DELETE`
- **Authorization**: 관리자 권한 필요 (`hasRole('ADMIN')`)
- **Response**: `204 No Content`

---

## 2. 사용자 권한 관리 API

### 2.1. 사용자 권한 목록 조회
특정 사용자의 세부 권한 목록을 조회합니다.

- **URL**: `/api/v1/users/{userId}/permissions`
- **Method**: `GET`
- **Authorization**: 관리자 권한 필요 (`hasRole('ADMIN')`)
- **Response**: `200 OK` (List<RoleResponse>)

### 2.2. 사용자 권한 추가
사용자에게 특정 세부 권한을 추가합니다.

- **URL**: `/api/v1/users/{userId}/permissions`
- **Method**: `POST`
- **Authorization**: 관리자 권한 필요 (`hasRole('ADMIN')`)
- **Request Body**:
  ```json
  {
    "permissionCode": "classroom:write:12"
  }
  ```
- **Response**: `200 OK` (갱신된 권한 목록)

### 2.3. 사용자 권한 제거
사용자의 특정 세부 권한을 제거합니다.

- **URL**: `/api/v1/users/{userId}/permissions`
- **Method**: `DELETE`
- **Authorization**: 관리자 권한 필요 (`hasRole('ADMIN')`)
- **Request Body**:
  ```json
  {
    "permissionCode": "classroom:write:12"
  }
  ```
- **Response**: `200 OK` (갱신된 권한 목록)
