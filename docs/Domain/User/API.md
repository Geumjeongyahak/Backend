# User Domain API

사용자 계정 조회·생성·수정·삭제와 세부 권한(permission) 관리를 위한 API 명세입니다.

---

## 공통 사항

### 기본 URL
```
/api/v1/users
```

### 인증
모든 엔드포인트는 JWT Bearer 토큰이 필요합니다.
```
Authorization: Bearer <access_token>
```

### 응답 형식

**성공**
```json
{ "data": { ... } }
```

**실패**
```json
{
  "errorCode": "RES-01-001",
  "message": "사용자를 찾을 수 없습니다."
}
```

### 권한 조건 표기

| 표기 | 의미 |
|------|------|
| `ADMIN` | 기본 역할 ADMIN |
| `user:read:*` | 사용자 조회 권한 |
| `user:write:*` | 사용자 생성 권한 |
| `user:manage:*` | 사용자 수정/삭제 권한 |
| `user:grant:*` | 사용자 직접 권한 조회/부여/회수 권한 |
| `ADMIN \| user:manage:*` | 둘 중 하나 이상 보유 |
| `인증만` | 로그인 상태면 누구든 가능 |

---

## 1. User Admin API

관리자/운영자가 전체 사용자를 관리하는 API입니다.

---

### 1.1 사용자 목록 조회

```
GET /api/v1/users
```

**권한**: `ADMIN` | `user:read:*`

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `page` | integer | N | `0` | 페이지 번호 (0-based) |
| `size` | integer | N | `20` | 페이지당 항목 수 |

#### Response `200 OK`

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
  "totalElements": 42,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

#### Error

| 코드 | HTTP | 설명 |
|------|------|------|
| `AUTHZ001` | 403 | 권한 없음 |

---

### 1.2 사용자 상세 조회

```
GET /api/v1/users/{userId}
```

**권한**: `ADMIN` | `user:read:*`

#### Path Parameters

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `userId` | Long | 사용자 ID |

#### Response `200 OK`

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
    { "name": "lesson:write:*", "code": "lesson:write:*", "source": "MANUAL" },
    { "name": "channel:write:15", "code": "channel:write:15", "source": "MEMBER" }
  ],
  "createdAt": "2024-01-01T12:00:00",
  "updatedAt": "2024-01-02T15:30:00"
}
```

#### Error

| 코드 | HTTP | 설명 |
|------|------|------|
| `RES-01-001` | 404 | 사용자를 찾을 수 없음 |
| `AUTHZ001` | 403 | 권한 없음 |

---

### 1.3 사용자 생성

```
POST /api/v1/users
```

**권한**: `ADMIN` | `user:write:*`

#### Request Body

```json
{
  "email": "newuser@example.com",
  "name": "홍길동",
  "password": "password123!",
  "phoneNumber": "010-1234-5678",
  "role": "VOLUNTEER",
  "departmentId": 1
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `email` | string | **Y** | 이메일 형식 | 로그인용 이메일. 중복 불가 |
| `name` | string | **Y** | 최대 50자 | 실명 또는 관리용 이름 |
| `password` | string | **Y** | 최소 8자 | 초기 로그인 비밀번호 |
| `phoneNumber` | string | N | 전화번호 형식 | 연락처 |
| `role` | string | N | `ADMIN`, `MANAGER`, `VOLUNTEER`, `GUEST` | 기본 역할. 미입력 시 `VOLUNTEER` |
| `departmentId` | Long | N | - | 소속 부서 ID |

#### Response `201 Created`

`UserDetailResponse` (1.2 상세 조회 응답과 동일한 구조)

#### Side Effect

- `users` 테이블에 사용자 레코드 생성
- `user_credentials` 테이블에 LOCAL 로그인 자격 증명 생성

#### Error

| 코드 | HTTP | 설명 |
|------|------|------|
| `BIZ-01-002` | 409 | 이메일 중복 |
| `RES-02-001` | 404 | 부서를 찾을 수 없음 (`departmentId` 지정 시) |
| `VAL001` | 400 | 입력값 검증 실패 |
| `AUTHZ001` | 403 | 권한 없음 |

---

### 1.4 사용자 수정 (관리자)

```
PATCH /api/v1/users/{userId}
```

**권한**: `ADMIN` | `user:manage:*`

전달한 필드만 반영합니다. 변경하지 않을 필드는 요청에서 생략하세요.

#### Path Parameters

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `userId` | Long | 사용자 ID |

#### Request Body

```json
{
  "name": "수정된이름",
  "phoneNumber": "010-9876-5432",
  "email": "changed@example.com",
  "password": "newpassword123!",
  "role": "MANAGER",
  "departmentId": 3
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `name` | string | N | 최대 50자 | 이름 |
| `phoneNumber` | string | N | 전화번호 형식 | 연락처 |
| `email` | string | N | 이메일 형식 | 로그인 이메일. 변경 시 credential도 함께 변경됨 |
| `password` | string | N | 최소 8자 | 비밀번호. 전달 시 credential 해시 갱신 |
| `role` | string | N | `ADMIN`, `MANAGER`, `VOLUNTEER`, `GUEST` | 기본 역할 |
| `departmentId` | Long | N | - | 소속 부서 ID |

> `role`을 `GUEST`로 변경하면 교원 해제로 처리합니다.
> 이 경우 소속 부서와 배정 분반은 `null`로 비워지고, `teacherEndAt`은 처리일로 설정되며, `user_permissions`의 직접 권한은 모두 삭제됩니다.
> 같은 요청에 `departmentId` 또는 `classroomId`를 함께 보내도 소속/분반은 비워진 상태로 유지됩니다.

#### Response `200 OK`

`UserDetailResponse` (1.2 상세 조회 응답과 동일한 구조)

#### Side Effect

- `users` 테이블 기본 정보 변경
- `email` 변경 시: `user_credentials` 테이블의 credential_email 함께 갱신
- `password` 변경 시: `user_credentials` 테이블의 password_hash 갱신
- `role`/`departmentId` 변경 시: 해당 사용자의 인가 범위에 즉시 영향
- `role`을 `GUEST`로 변경 시: 교원 해제 처리, 소속/분반 제거, `teacherEndAt` 설정, `user_permissions` 직접 권한 전체 삭제

#### Error

| 코드 | HTTP | 설명 |
|------|------|------|
| `RES-01-001` | 404 | 사용자를 찾을 수 없음 |
| `BIZ-01-002` | 409 | 이메일 중복 |
| `RES-02-001` | 404 | 부서를 찾을 수 없음 |
| `VAL001` | 400 | 입력값 검증 실패 |
| `AUTHZ001` | 403 | 권한 없음 |

---

### 1.5 사용자 삭제

```
DELETE /api/v1/users/{userId}
```

**권한**: `ADMIN` | `user:manage:*`

#### Path Parameters

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `userId` | Long | 사용자 ID |

#### Response `204 No Content`

#### Side Effect

- `users` 테이블에서 사용자 레코드 삭제
- CASCADE: `user_credentials`, `user_permissions` 연관 레코드 함께 삭제
- 삭제 후 해당 계정으로의 로그인 및 조회 불가

#### Error

| 코드 | HTTP | 설명 |
|------|------|------|
| `RES-01-001` | 404 | 사용자를 찾을 수 없음 |
| `AUTHZ001` | 403 | 권한 없음 |

---

## 2. User Self API

현재 로그인한 사용자가 본인 정보를 조회·수정하는 API입니다.

---

### 2.1 본인 조회

```
GET /api/v1/users/me
```

**권한**: `인증만`

#### Response `200 OK`

`UserDetailResponse` (1.2 상세 조회 응답과 동일한 구조)

#### Error

| 코드 | HTTP | 설명 |
|------|------|------|
| `AUTH001` | 401 | 인증 실패 |

---

### 2.2 본인 수정

```
PATCH /api/v1/users/me
```

**권한**: `인증만`

전달한 필드만 반영합니다. `role`, `departmentId`는 수정할 수 없습니다.

#### Request Body

```json
{
  "name": "수정된이름",
  "phoneNumber": "010-9876-5432",
  "email": "changed@example.com",
  "password": "newpassword123!"
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `name` | string | N | 최대 50자 | 이름 |
| `phoneNumber` | string | N | 전화번호 형식 | 연락처 |
| `email` | string | N | 이메일 형식 | 로그인 이메일. 변경 시 credential도 함께 변경됨 |
| `password` | string | N | 최소 8자 | 비밀번호. 전달 시 credential 해시 갱신 |

#### Response `200 OK`

`UserDetailResponse` (1.2 상세 조회 응답과 동일한 구조)

#### Side Effect

- `users` 테이블 본인 프로필 정보 변경
- `email` 변경 시: `user_credentials` 테이블의 credential_email 함께 갱신
- `password` 변경 시: `user_credentials` 테이블의 password_hash 갱신

#### Error

| 코드 | HTTP | 설명 |
|------|------|------|
| `BIZ-01-002` | 409 | 이메일 중복 |
| `VAL001` | 400 | 입력값 검증 실패 |
| `AUTH001` | 401 | 인증 실패 |

---

## 3. User Permission API

사용자에게 직접 부여된 세부 권한(authority)을 관리하는 API입니다.
기본 역할(role) 기반 권한과 별개로 예외적 운영 권한을 부여할 때 사용합니다.

---

### 3.1 권한 목록 조회

```
GET /api/v1/users/{userId}/permissions
```

**권한**: `ADMIN` | `user:read:*` | `user:grant:*`

사용자에게 직접 저장된 permission code만 반환합니다. role 기반 권한과 부서 직책 권한은 포함되지 않습니다.

#### Path Parameters

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `userId` | Long | 사용자 ID |

#### Response `200 OK`

```json
[
  { "name": "user:manage:*", "code": "user:manage:*", "source": "MANUAL" },
  { "name": "lesson:write:*", "code": "lesson:write:*", "source": "MANUAL" }
]
```

#### Error

| 코드 | HTTP | 설명 |
|------|------|------|
| `RES-01-001` | 404 | 사용자를 찾을 수 없음 |
| `AUTHZ001` | 403 | 권한 없음 |

---

### 3.2 권한 추가

```
POST /api/v1/users/{userId}/permissions
```

**권한**: `ADMIN` | `user:grant:*`

이미 동일한 권한이 존재하면 중복 저장하지 않고 현재 목록을 그대로 반환합니다.

#### Path Parameters

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `userId` | Long | 사용자 ID |

#### Request Body

```json
{
  "permissionCode": "user:manage:*"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `permissionCode` | string | **Y** | 추가할 권한 코드 |

#### Response `200 OK`

갱신된 전체 권한 목록 (3.1 응답과 동일한 구조)

#### Side Effect

- `user_permissions` 테이블에 권한 레코드 추가 (이미 있으면 무시)
- 이후 해당 사용자의 인가 결과에 즉시 영향

#### Error

| 코드 | HTTP | 설명 |
|------|------|------|
| `RES-01-001` | 404 | 사용자를 찾을 수 없음 |
| `VAL001` | 400 | 유효하지 않은 permissionCode 형식 |
| `AUTHZ001` | 403 | 권한 없음 |

---

### 3.3 권한 제거

```
DELETE /api/v1/users/{userId}/permissions
```

**권한**: `ADMIN` | `user:grant:*`

요청한 권한이 존재하지 않아도 오류 없이 현재 목록을 반환합니다.

#### Path Parameters

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `userId` | Long | 사용자 ID |

#### Request Body

```json
{
  "permissionCode": "user:manage:*"
}
```

#### Response `200 OK`

갱신된 전체 권한 목록 (3.1 응답과 동일한 구조)

#### Side Effect

- `user_permissions` 테이블에서 해당 권한 레코드 삭제 (없으면 무시)
- 이후 해당 사용자의 인가 결과에 즉시 영향

#### Error

| 코드 | HTTP | 설명 |
|------|------|------|
| `RES-01-001` | 404 | 사용자를 찾을 수 없음 |
| `VAL001` | 400 | 유효하지 않은 permissionCode 형식 |
| `AUTHZ001` | 403 | 권한 없음 |

---

## 4. 공통 모델

### UserSimpleResponse

사용자 목록 조회(1.1)에서 사용하는 축약 응답입니다.

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `id` | Long | N | 사용자 식별자 |
| `name` | string | N | 이름 |
| `email` | string | N | 기본 이메일 |
| `phoneNumber` | string | Y | 전화번호 |
| `role` | string | N | 기본 역할 (`ADMIN`, `MANAGER`, `VOLUNTEER`, `GUEST`) |
| `departmentId` | Long | Y | 소속 부서 ID |

### UserDetailResponse

사용자 상세/생성/수정 응답에 공통으로 사용하는 응답입니다.

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `id` | Long | N | 사용자 식별자 |
| `name` | string | N | 이름 |
| `email` | string | N | 기본 이메일 |
| `phoneNumber` | string | Y | 전화번호 |
| `role` | string | N | 기본 역할 |
| `department` | `DepartmentSimpleResponse` | Y | 소속 부서. 없거나 교원 해제 상태이면 null |
| `classroom` | `ClassroomSummaryResponse` | Y | 배정 분반. 없거나 교원 해제 상태이면 null |
| `teacherStartAt` | date | Y | 교원 활동 시작일 |
| `teacherEndAt` | date | Y | 교원 활동 종료일. 교원 해제 시 처리일로 설정 |
| `permissions` | `PermissionResponse[]` | N | 직접 권한과 부서 직책 권한 목록 |
| `createdAt` | datetime | N | 계정 생성 일시 (ISO 8601) |
| `updatedAt` | datetime | N | 마지막 수정 일시 (ISO 8601) |

### PermissionResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | string | 권한 표시명 (현재 code와 동일) |
| `code` | string | 권한 코드 (예: `user:manage:*`, `lesson:write:*`) |
| `source` | string | 권한 출처. 직접 권한은 `MANUAL`, 부서 권한은 `MEMBER` 또는 `MANAGER` |

### RoleType

| 값 | 설명 |
|----|------|
| `ADMIN` | 관리자 |
| `MANAGER` | 매니저 |
| `VOLUNTEER` | 봉사자 |
| `GUEST` | 게스트 |
