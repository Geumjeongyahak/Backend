# User API

사용자 정보 조회, 생성, 수정, 삭제 및 부서/권한 관리를 위한 API 명세입니다.

---

## 1. 사용자 관리 API

### 1.0. 교사 연락망 조회
현재 활동 중인 교사 목록을 연락망 화면용으로 조회합니다.

- **URL**: `/api/v1/teachers/contact-list`
- **Method**: `GET`
- **Authorization**: 교사 이상 권한 필요 (`VOLUNTEER`, `MANAGER`, `ADMIN`)
- **Response**: `200 OK` (`List<TeacherContactResponse>`)
  ```json
  [
    {
      "id": 2,
      "name": "홍길동",
      "classroomName": "국화반",
      "phoneNumber": "010-1234-5678"
    }
  ]
  ```

조회 기준:
- `teacherStartAt`이 오늘 이전 또는 오늘인 사용자
- `teacherEndAt`이 없거나 오늘 이후 또는 오늘인 사용자

### 1.1. 사용자 목록 조회
전체 사용자 목록을 페이지네이션 및 검색 조건에 따라 조회합니다.

- **URL**: `/api/v1/users`
- **Method**: `GET`
- **Authorization**: `ADMIN` 또는 `user:read:*`
- **Query Parameters**:
    - `page` (optional): 페이지 번호 (0-based)
    - `size` (optional): 페이지 크기
    - `role` (optional): 역할 필터 (`ADMIN`, `MANAGER`, `VOLUNTEER`, `GUEST`)
    - `name` (optional): 이름 부분 검색
    - `currentTeacher` (optional): `true` 설정 시 현재 활동 기간 내의 교원만 조회
- **Response**: `200 OK` (PaginationResponse<UserSimpleResponse>)
  ```json
  {
    "content": [
      {
        "id": 1,
        "name": "홍길동",
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
특정 사용자의 상세 정보를 조회합니다. 직접 권한과 부서 직책 권한을 합친 권한 목록을 포함합니다.

- **URL**: `/api/v1/users/{userId}`
- **Method**: `GET`
- **Authorization**: `ADMIN` 또는 `user:read:*`
- **Path Parameters**:
  - `userId` (Long): 사용자 ID
- **Response**: `200 OK` (UserDetailResponse)
  ```json
  {
    "id": 1,
    "name": "홍길동",
    "email": "user@example.com",
    "phoneNumber": "010-1234-5678",
    "role": "VOLUNTEER",
    "department": {
      "id": 2,
      "name": "교육연구부",
      "description": "교육 프로그램 연구..."
    },
    "classroom": null,
    "permissions": [
      { "name": "lesson:write:1", "code": "lesson:write:1", "source": "MANUAL" },
      { "name": "channel:write:15", "code": "channel:write:15", "source": "MEMBER" }
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
- **Authorization**: `ADMIN` 또는 `user:write:*`
- **Request Body**: (CreateUserRequest)
  ```json
  {
    "email": "newuser@test.com",
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
- **Authorization**: `ADMIN` 또는 `user:manage:*`
- **Request Body**: (UpdateUserRequest)
  ```json
  {
    "name": "수정된이름",
    "role": "MANAGER",
    "departmentId": 3
  }
  ```
- **Response**: `200 OK` (UserDetailResponse)
- **Side Effect**:
  - `role`을 `GUEST`로 변경하면 교원 해제로 처리합니다.
  - 교원 해제 시 `departmentId`, `classroomId`는 `null`이 되고 `teacherEndAt`은 처리일로 설정됩니다.
  - 교원 해제 시 `user_permissions`의 직접 권한이 모두 삭제됩니다.
  - `role=GUEST` 요청에 `departmentId`나 `classroomId`를 함께 보내도 소속/분반은 비워진 상태로 유지됩니다.

### 1.6. 사용자 본인 수정
사용자가 자신의 정보를 직접 수정합니다. 역할 및 부서 정보는 수정할 수 없습니다.

- **URL**: `/api/v1/users/me`
- **Method**: `PATCH`
- **Authorization**: 인증 필요 (`isAuthenticated()`)
- **Request Body**: (UpdateSelfRequest)
  ```json
  {
    "password": "newpassword123!"
  }
  ```
- **Response**: `200 OK` (UserDetailResponse)

### 1.7. 사용자 삭제
사용자 계정을 삭제합니다.

- **URL**: `/api/v1/users/{userId}`
- **Method**: `DELETE`
- **Authorization**: `ADMIN` 또는 `user:manage:*`
- **Response**: `204 No Content`

---

## 2. 사용자 권한 관리 API

### 2.1. 사용자 권한 목록 조회
특정 사용자에게 직접 부여된 수동 예외 권한 목록을 조회합니다.

- **URL**: `/api/v1/users/{userId}/permissions`
- **Method**: `GET`
- **Authorization**: `ADMIN` 또는 `user:read:*` 또는 `user:grant:*`
- **Response**: `200 OK` (List<PermissionResponse>)
- **Note**:
  - `user_permissions`에 저장된 직접 권한만 반환합니다.
  - 부서 직책 권한(`source=MEMBER`, `source=MANAGER`)은 사용자 상세 조회 응답에서 확인합니다.

### 2.2. 사용자 권한 추가
사용자에게 수동 예외 권한을 추가합니다.

- **URL**: `/api/v1/users/{userId}/permissions`
- **Method**: `POST`
- **Authorization**: `ADMIN` 또는 `user:grant:*`
- **Request Body**:
  ```json
  {
    "permissionCode": "classroom:write:12"
  }
  ```
- **Response**: `200 OK` (갱신된 권한 목록)
- **Side Effect**:
  - `user_permissions`에 직접 권한이 추가됩니다.
  - 응답의 `source`는 `MANUAL`입니다.

### 2.3. 사용자 권한 제거
사용자의 특정 수동 예외 권한을 제거합니다.

- **URL**: `/api/v1/users/{userId}/permissions`
- **Method**: `DELETE`
- **Authorization**: `ADMIN` 또는 `user:grant:*`
- **Request Body**:
  ```json
  {
    "permissionCode": "classroom:write:12"
  }
  ```
- **Response**: `200 OK` (갱신된 권한 목록)
- **Side Effect**:
  - `user_permissions`에서 직접 권한이 제거됩니다.
