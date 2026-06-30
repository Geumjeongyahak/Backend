# Classroom API

Classroom은 서비스에서 “교실”이 아니라 “분반/교육반”을 의미합니다.

## 공통 정책

- Base URL: `/api/v1/classrooms`
- 목록/상세 조회는 인증 없이 호출할 수 있습니다. (2026-05-22 보안 정책 업데이트: 상세 조회 엔드포인트에 대한 public access 보장)
- 생성/수정/삭제는 `ADMIN` 또는 `MANAGER` 권한이 필요합니다.
- 성공 응답은 별도 래핑 없이 DTO를 직접 반환합니다.
- 삭제는 soft delete 방식이며, 삭제된 분반은 목록/상세 조회에서 제외됩니다.
- 분반 생성 시 도메인 연동 Classroom 채널이 자동 생성됩니다.
- 분반 삭제 시 도메인 연동 Classroom 채널은 삭제하지 않고 `isActive=false`, `isDeleted=false`로 비활성화합니다.
- `type` 값은 `WEEKDAY`(주중반), `WEEKEND`(주말반)를 사용합니다.

## 1. 분반 목록 조회

- **URL**: `/api/v1/classrooms`
- **Method**: `GET`
- **Auth**: 불필요
- **Description**: 삭제되지 않은 분반 목록을 페이지네이션으로 조회합니다.

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| page | number | N | 0 | 페이지 번호, 0부터 시작 |
| size | number | N | 10 | 페이지 크기 |
| name | string | N | 없음 | 분반 이름 부분 검색 |
| type | string | N | 없음 | `WEEKDAY`, `WEEKEND` |
| sort | string | N | 없음 | 정렬 조건 |

### Response: 200 OK

```json
{
  "content": [
    {
      "id": 1,
      "name": "해바라기반",
      "type": "WEEKDAY"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

## 2. 분반 상세 조회

- **URL**: `/api/v1/classrooms/{id}`
- **Method**: `GET`
- **Auth**: 불필요
- **Description**: 특정 분반의 상세 정보를 조회합니다.

### Response: 200 OK

```json
{
  "id": 1,
  "name": "해바라기반",
  "type": "WEEKDAY",
  "description": "초등 수준의 기초 교육을 제공하는 분반",
  "createdAt": "2026-05-05T18:30:00",
  "updatedAt": "2026-05-05T18:30:00"
}
```

## 3. 분반 생성

- **URL**: `/api/v1/classrooms`
- **Method**: `POST`
- **Auth**: 필요
- **Roles**: `ADMIN`, `MANAGER`
- **Description**: 새로운 분반을 생성합니다.

### Request Body

```json
{
  "name": "해바라기반",
  "type": "WEEKDAY",
  "description": "초등 수준의 기초 교육을 제공하는 분반"
}
```

### Request Fields

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| name | string | Y | 2자 이상 50자 이하, 중복 불가 |
| type | string | Y | `WEEKDAY`, `WEEKEND` |
| description | string | N | 분반 설명 |

### Response: 201 Created

```json
{
  "id": 1,
  "name": "해바라기반",
  "type": "WEEKDAY",
  "description": "초등 수준의 기초 교육을 제공하는 분반",
  "createdAt": "2026-05-05T18:30:00",
  "updatedAt": "2026-05-05T18:30:00"
}
```

## 4. 분반 수정

- **URL**: `/api/v1/classrooms/{id}`
- **Method**: `PATCH`
- **Auth**: 필요
- **Roles**: `ADMIN`, `MANAGER`
- **Description**: 기존 분반 정보를 부분 수정합니다.

### Request Body

```json
{
  "name": "해바라기반",
  "type": "WEEKEND",
  "description": "수정된 설명"
}
```

### Request Fields

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| name | string | N | 2자 이상 50자 이하, 다른 분반 이름과 중복 불가 |
| type | string | N | `WEEKDAY`, `WEEKEND` |
| description | string | N | 값을 생략하면 기존 설명 유지, 빈 문자열 `""` 전달 시 설명 비움 |

전달된 필드만 변경합니다. 변경되는 값이 하나도 없는 경우 에러를 반환하지 않고 기존 리소스를 그대로 반환합니다.

### Response: 200 OK

```json
{
  "id": 1,
  "name": "해바라기반",
  "type": "WEEKEND",
  "description": "수정된 설명",
  "createdAt": "2026-05-05T18:30:00",
  "updatedAt": "2026-05-05T18:40:00"
}
```

## 5. 분반 삭제

- **URL**: `/api/v1/classrooms/{id}`
- **Method**: `DELETE`
- **Auth**: 필요
- **Roles**: `ADMIN`, `MANAGER`
- **Description**: 분반을 삭제 처리합니다.

### Side Effects

- `classrooms.is_deleted`가 `true`로 변경됩니다.
- 연결된 `CLASSROOM + DOMAIN_LINKED` 채널은 `isActive=false`로 변경됩니다.
- 연결 채널의 `isDeleted` 값은 `false`로 유지됩니다.
- 기존 게시글은 삭제하거나 이관하지 않고 비활성 채널에 보관합니다.

### Response: 204 No Content

응답 body는 없습니다.
