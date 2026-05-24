# Teacher Application API

교원 신청서는 GUEST 사용자가 교원 활동을 지원하기 위해 제출하는 신청 데이터입니다.
관리자는 신청서를 검토한 뒤 승인 또는 반려하며, 승인 시 교원 활동 분반과 활동 기간을 함께 확정합니다.

## 공통 정책

- 일반 사용자 Base URL: `/api/v1/teacher-applications`
- 관리자 Base URL: `/api/v1/admin/teacher-applications`
- 교원 신청 제출은 `GUEST` 역할 사용자만 가능합니다.
- 한 사용자는 `PENDING` 신청을 1개만 가질 수 있습니다.
- `REJECTED`, `CANCELLED` 신청은 재신청을 막지 않습니다.
- 신청 시점의 개인정보를 snapshot으로 저장합니다.
- 선호 과목은 교사가 미배정된 활성 과목만 선택할 수 있습니다.
- 선호 과목은 자동 배정되지 않습니다. 승인 시에도 과목 담당 교사 배정은 하지 않습니다.
- 승인 시 신청자의 사용자 ID는 유지되고, User 도메인 이벤트 처리로 교원 활동 정보가 등록됩니다.
- 승인 시 새 분반 채널 쓰기 권한을 추가하며, 기존 분반 권한은 제거하지 않습니다.

## 상태

| 값 | 의미 |
|---|---|
| `PENDING` | 검토 대기 |
| `APPROVED` | 승인 완료 |
| `REJECTED` | 반려 완료 |
| `CANCELLED` | 신청자 취소 |

## TeacherApplicationResponse

```json
{
  "id": 1,
  "applicantId": 5,
  "applicantName": "홍길동",
  "applicantPhoneNumber": "010-0000-0000",
  "applicantEmail": "hong@example.com",
  "birthDate": "1999-03-15",
  "address": "부산광역시 금정구",
  "educationAndMajor": "부산대학교 국어국문학과 졸업",
  "preferredSubjectId": 3,
  "preferredSubjectName": "국어",
  "preferredClassroomName": "국화반",
  "preferredDayOfWeek": "FRIDAY",
  "preferredStartTime": "19:00:00",
  "preferredEndTime": "22:00:00",
  "motivation": "지원 동기",
  "desiredTeacherImage": "희망하는 교사상",
  "meaningOfSharing": "나눔의 의미",
  "status": "PENDING",
  "reviewedAt": null,
  "reviewedByName": null,
  "reviewNote": null,
  "createdAt": "2026-05-20T10:00:00",
  "updatedAt": "2026-05-20T10:00:00"
}
```

## 1. 교원 신청서 제출

- **URL**: `/api/v1/teacher-applications`
- **Method**: `POST`
- **Auth**: 필요
- **Roles**: `GUEST`
- **Description**: 로그인한 GUEST 사용자가 교원 신청서를 제출합니다.

### Request Body

```json
{
  "birthDate": "1999-03-15",
  "phoneNumber": "010-0000-0000",
  "email": "hong@example.com",
  "address": "부산광역시 금정구",
  "educationAndMajor": "부산대학교 국어국문학과 졸업",
  "preferredSubjectId": 3,
  "motivation": "지원 동기",
  "desiredTeacherImage": "희망하는 교사상",
  "meaningOfSharing": "나눔의 의미"
}
```

### 구현 기준 동작

- 신청자 이름은 현재 User 이름으로 snapshot 저장합니다.
- 연락처, 이메일, 생년월일, 주소, 학력/전공은 신청서에 snapshot 저장합니다.
- 같은 사용자의 `PENDING` 신청이 이미 있으면 생성할 수 없습니다.
- 선호 과목이 비활성 과목이거나 담당 교사가 이미 있으면 생성할 수 없습니다.

## 2. 내 교원 신청 조회

- **URL**: `/api/v1/teacher-applications/me`
- **Method**: `GET`
- **Auth**: 필요
- **Description**: 현재 로그인 사용자의 최신 신청 1건을 조회합니다.

### 조회 정책

- `PENDING`, `APPROVED`, `REJECTED` 중 최신 1건을 반환합니다.
- `CANCELLED` 신청은 반환하지 않습니다.
- 신청이 없으면 `404`가 아니라 `exists=false`를 반환합니다.

### Response Body

```json
{
  "exists": true,
  "application": {
    "id": 1,
    "status": "PENDING"
  }
}
```

신청이 없는 경우:

```json
{
  "exists": false,
  "application": null
}
```

## 3. 교원 신청 수정

- **URL**: `/api/v1/teacher-applications/{applicationId}`
- **Method**: `PATCH`
- **Auth**: 필요
- **Description**: 신청자 본인이 `PENDING` 상태의 신청서를 수정합니다.

### Request Body

제출 요청과 동일한 전체 신청서 필드를 전달합니다.

```json
{
  "birthDate": "1999-03-15",
  "phoneNumber": "010-1111-2222",
  "email": "hong.updated@example.com",
  "address": "부산광역시 금정구",
  "educationAndMajor": "부산대학교 국어국문학과 졸업",
  "preferredSubjectId": 4,
  "motivation": "수정된 지원 동기",
  "desiredTeacherImage": "수정된 교사상",
  "meaningOfSharing": "수정된 나눔의 의미"
}
```

### 구현 기준 동작

- 요청자가 신청자가 아니면 수정할 수 없습니다.
- `PENDING` 상태가 아니면 수정할 수 없습니다.
- 수정 시에도 선호 과목은 교사가 미배정된 활성 과목이어야 합니다.

## 4. 교원 신청 취소

- **URL**: `/api/v1/teacher-applications/{applicationId}`
- **Method**: `DELETE`
- **Auth**: 필요
- **Description**: 신청자 본인이 `PENDING` 상태의 신청서를 취소합니다.

### Side Effects

- 물리 삭제하지 않고 상태를 `CANCELLED`로 변경합니다.
- 취소된 신청은 내 신청 조회에서 제외됩니다.
- 취소 후 재신청할 수 있습니다.

## 5. 관리자 교원 신청 목록 조회

- **URL**: `/api/v1/admin/teacher-applications`
- **Method**: `GET`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `teacher-application:read:*`
- **Description**: 교원 신청 목록을 페이지네이션으로 조회합니다.

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| status | string | N | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| keyword | string | N | 신청자 이름, 이메일, 연락처 검색 |
| page | number | N | 페이지 번호, 기본값 0 |
| size | number | N | 페이지 크기, 기본값 10 |

### 정렬

- 최신순 고정 정렬입니다.
- `createdAt DESC`, `id DESC` 순서로 조회합니다.

## 6. 관리자 교원 신청 상세 조회

- **URL**: `/api/v1/admin/teacher-applications/{applicationId}`
- **Method**: `GET`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `teacher-application:read:*`
- **Description**: 관리자 권한으로 교원 신청 상세 정보를 조회합니다.

## 7. 관리자 교원 신청 승인

- **URL**: `/api/v1/admin/teacher-applications/{applicationId}/approve`
- **Method**: `PATCH`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `teacher-application:manage:*`
- **Description**: `PENDING` 신청을 승인하고 교원 활동 정보를 확정합니다.

### Request Body

```json
{
  "classroomId": 2,
  "teacherStartAt": "2026-06-01",
  "teacherEndAt": "2026-12-31",
  "note": "면접 후 승인"
}
```

`note`는 선택 값입니다.

### Side Effects

- 신청 상태를 `APPROVED`로 변경합니다.
- `reviewedAt`, `reviewedBy`, `reviewNote`를 기록합니다.
- 신청자가 승인 시점에도 `GUEST`인지 확인합니다.
- `TeacherApprovedEvent`를 발행합니다.
- User 도메인 이벤트 핸들러가 신청자의 `approveTeacherProfile()`을 호출합니다.
- 신청자 역할이 `VOLUNTEER`로 승격됩니다.
- 신청자의 교원 활동 분반, 시작일, 종료일이 저장됩니다.
- 새 분반 채널 쓰기 권한이 추가됩니다.
- 선호 과목 담당 교사는 자동 배정하지 않습니다.

## 8. 관리자 교원 신청 반려

- **URL**: `/api/v1/admin/teacher-applications/{applicationId}/reject`
- **Method**: `PATCH`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `teacher-application:manage:*`
- **Description**: `PENDING` 신청을 반려합니다.

### Request Body

```json
{
  "note": "면접 일정 미참석"
}
```

`note`는 필수입니다.

### Side Effects

- 신청 상태를 `REJECTED`로 변경합니다.
- `reviewedAt`, `reviewedBy`, `reviewNote`를 기록합니다.
- 신청자 역할은 변경하지 않습니다.
- 반려 후 재신청할 수 있습니다.

## 대표 실패 케이스

| 상황 | HTTP Status |
|---|---|
| 인증 없이 접근 | `401 Unauthorized` |
| 권한 없는 사용자가 관리자 API 접근 | `403 Forbidden` |
| GUEST가 아닌 사용자가 신청서 제출 | `403 Forbidden` |
| 승인 시 신청자가 더 이상 GUEST가 아님 | `403 Forbidden` |
| 존재하지 않는 신청서 | `404 Not Found` |
| 승인 시 존재하지 않는 분반 | `404 Not Found` |
| 이미 PENDING 신청이 있음 | `409 Conflict` |
| PENDING이 아닌 신청 수정/취소/승인/반려 | `409 Conflict` |
| 선호 과목이 비활성 또는 이미 교사 배정됨 | `400 Bad Request` |
| 승인 요청의 교원 활동 종료일이 시작일보다 빠름 | `400 Bad Request` |

## 대표 시퀀스

```mermaid
sequenceDiagram
    actor Applicant as 신청자
    actor Admin as 관리자
    participant ApplicationAPI as TeacherApplicationController
    participant AdminAPI as TeacherApplicationAdminController
    participant ApplicationService as TeacherApplicationService
    participant UserHandler as TeacherApplicationEventHandler
    participant User as User

    Applicant->>ApplicationAPI: POST /teacher-applications
    ApplicationAPI->>ApplicationService: createTeacherApplication(...)
    ApplicationService-->>Applicant: PENDING 신청 생성

    Admin->>AdminAPI: PATCH /admin/teacher-applications/{id}/approve
    AdminAPI->>ApplicationService: approveTeacherApplication(...)
    ApplicationService->>ApplicationService: 신청 APPROVED 처리
    ApplicationService->>UserHandler: TeacherApprovedEvent 발행
    UserHandler->>User: approveTeacherProfile(...)
    ApplicationService-->>Admin: APPROVED 신청 반환
```
