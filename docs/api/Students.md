# Student API

Student는 야학에 등록된 학생을 의미합니다. 학생은 하나의 분반에 소속됩니다.

## 공통 정책

- Base URL: `/api/v1/students`
- 성공 응답은 별도 래핑 없이 DTO를 직접 반환합니다.
- 목록/상세 조회는 교원 이상 권한(`VOLUNTEER`, `MANAGER`, `ADMIN`)이 필요합니다.
- 생성/수정/삭제는 `ADMIN` 또는 `student:write:*` 권한이 필요합니다.
- 삭제는 soft delete 방식이며, 삭제된 학생은 목록/상세 조회에서 제외됩니다.
- 학생 목록은 페이지네이션 없이 배열로 반환합니다.
- 학생 목록은 기본적으로 이름 오름차순(`name ASC`)으로 정렬됩니다.
- 존재하지 않거나 삭제된 `classroomId`가 전달되면 `404 Not Found`를 반환합니다.

## StudentResponse

```json
{
  "id": 1,
  "name": "홍길동",
  "phoneNumber": "010-1234-5678",
  "description": "기초반 학생",
  "classroomId": 1,
  "classroomName": "벚꽃반",
  "status": "ENROLLED"
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| id | number | 학생 식별자 |
| name | string | 학생 이름 |
| phoneNumber | string | 전화번호 |
| description | string | 설명 |
| classroomId | number | 소속 분반 식별자 |
| classroomName | string | 소속 분반 이름 |
| status | string | `ENROLLED`, `ON_LEAVE`, `COMPLETED` |

## 1. 학생 목록 조회

- **URL**: `/api/v1/students`
- **Method**: `GET`
- **Auth**: 필요
- **Roles**: `VOLUNTEER`, `MANAGER`, `ADMIN`
- **Description**: 삭제되지 않은 학생 목록을 배열로 조회합니다.

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| name | string | N | 학생 이름 부분 검색 |
| status | string | N | `ENROLLED`, `ON_LEAVE`, `COMPLETED` |
| classroomId | number | N | 특정 분반 소속 학생만 조회 |

### Response: 200 OK

```json
[
  {
    "id": 1,
    "name": "김영희",
    "phoneNumber": "010-1111-2222",
    "description": "기초반 학생",
    "classroomId": 1,
    "classroomName": "벚꽃반",
    "status": "ENROLLED"
  },
  {
    "id": 2,
    "name": "박철수",
    "phoneNumber": "010-3333-4444",
    "description": "스마트폰 수업 참여",
    "classroomId": 2,
    "classroomName": "장미반",
    "status": "ON_LEAVE"
  }
]
```

## 2. 학생 상세 조회

- **URL**: `/api/v1/students/{studentId}`
- **Method**: `GET`
- **Auth**: 필요
- **Roles**: `VOLUNTEER`, `MANAGER`, `ADMIN`
- **Description**: 삭제되지 않은 특정 학생의 상세 정보를 조회합니다.

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| studentId | number | Y | 학생 식별자 |

### Response: 200 OK

```json
{
  "id": 1,
  "name": "홍길동",
  "phoneNumber": "010-1234-5678",
  "description": "기초반 학생",
  "classroomId": 1,
  "classroomName": "벚꽃반",
  "status": "ENROLLED"
}
```

## 3. 학생 생성

- **URL**: `/api/v1/students`
- **Method**: `POST`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `student:write:*`
- **Description**: 새로운 학생을 생성합니다.

### Request Body

```json
{
  "name": "홍길동",
  "phoneNumber": "010-1234-5678",
  "description": "기초반 학생",
  "classroomId": 1
}
```

### Request Fields

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| name | string | Y | 50자 이하 |
| phoneNumber | string | N | 전화번호 형식 |
| description | string | N | 학생 설명 |
| classroomId | number | Y | 소속 분반 식별자 |

### Response: 201 Created

```json
{
  "id": 1,
  "name": "홍길동",
  "phoneNumber": "010-1234-5678",
  "description": "기초반 학생",
  "classroomId": 1,
  "classroomName": "벚꽃반",
  "status": "ENROLLED"
}
```

## 4. 학생 수정

- **URL**: `/api/v1/students/{studentId}`
- **Method**: `PATCH`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `student:write:*`
- **Description**: 기존 학생 정보를 부분 수정합니다. `classroomId`를 전달하면 학생의 소속 분반을 변경합니다.

### Request Body

```json
{
  "name": "김철수",
  "phoneNumber": "010-4321-5678",
  "description": "수정된 설명",
  "status": "ON_LEAVE",
  "classroomId": 2
}
```

### Request Fields

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| name | string | N | 50자 이하 |
| phoneNumber | string | N | 전화번호 형식 |
| description | string | N | 학생 설명 |
| status | string | N | `ENROLLED`, `ON_LEAVE`, `COMPLETED` |
| classroomId | number | N | 변경할 소속 분반 식별자 |

전달된 필드만 변경합니다.

### Response: 200 OK

```json
{
  "id": 1,
  "name": "김철수",
  "phoneNumber": "010-4321-5678",
  "description": "수정된 설명",
  "classroomId": 2,
  "classroomName": "장미반",
  "status": "ON_LEAVE"
}
```

## 5. 학생 삭제

- **URL**: `/api/v1/students/{studentId}`
- **Method**: `DELETE`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `student:write:*`
- **Description**: 학생을 삭제 처리합니다.

삭제는 soft delete 방식입니다. 학생 레코드는 DB에 남지만, 삭제된 학생은 목록 조회와 상세 조회에서 제외됩니다.

### Response: 204 No Content

응답 body는 없습니다.
