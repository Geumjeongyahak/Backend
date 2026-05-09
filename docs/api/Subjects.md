# Subject API

Subject는 특정 분반의 특정 요일/교시 정기 수업 편성입니다.
하루 단위 운영 기록은 Lesson에 저장하며, Subject는 Lesson 자동 생성과 미래 수업 변경의 기준이 됩니다.

## 공통 정책

- Base URL: `/api/v1/subjects`
- 조회 API는 Subject API, 생성/수정/삭제 API는 Subject Admin API에서 제공합니다.
- 조회는 인증 사용자만 사용할 수 있습니다.
- 생성은 `ADMIN` 또는 `subject:write:*` 권한이 필요합니다.
- 수정/삭제는 `ADMIN` 또는 `subject:manage:*` 권한이 필요합니다.
- 담당 교사는 `VOLUNTEER` 또는 `MANAGER` 역할 사용자만 배정할 수 있습니다.
- 담당 교사가 없는 Subject는 생성할 수 있으며, 이 경우 Lesson을 자동 생성하지 않습니다.
- Lesson 자동 변경은 과거 Lesson을 건드리지 않고 미래 Lesson만 대상으로 합니다.
- 미래 Lesson 중 운영 기록이 없는 `SCHEDULED` 상태만 자동 변경 대상입니다.
- 운영 기록은 note, 학생 출석, 결석 요청, 진행 중인 수업 교환 요청/제안을 의미합니다.
- 이미 완료된 수업 교환 결과는 현재 Lesson 상태로 존중하며 자동 변경 차단 조건으로 보지 않습니다.

## SubjectDetailResponse

```json
{
  "id": 1,
  "classroomId": 1,
  "classroomName": "벚꽃반",
  "teacherId": 2,
  "teacherName": "홍길동",
  "name": "국어",
  "startAt": "2026-06-01",
  "endAt": "2026-08-30",
  "times": 12,
  "dayOfWeek": "MONDAY",
  "startTime": "19:20:00",
  "endTime": "20:00:00",
  "period": 1,
  "teacherAssignedAt": "2026-05-09T15:00:00",
  "description": "과목 설명"
}
```

`teacherId`, `teacherName`, `teacherAssignedAt`은 담당 교사가 아직 배정되지 않은 경우 `null`입니다.

## 1. 과목 목록 조회

- **URL**: `/api/v1/subjects`
- **Method**: `GET`
- **Auth**: 필요
- **Description**: 과목 목록을 배열로 조회합니다.

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| classroomId | number | N | 특정 분반의 과목만 조회 |

## 2. 과목 상세 조회

- **URL**: `/api/v1/subjects/{subjectId}`
- **Method**: `GET`
- **Auth**: 필요
- **Description**: 과목 상세 정보를 조회합니다.

## 3. 과목 생성

- **URL**: `/api/v1/subjects`
- **Method**: `POST`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `subject:write:*`

### Request Body

```json
{
  "classroomId": 1,
  "teacherId": 2,
  "name": "국어",
  "startAt": "2026-06-01",
  "endAt": "2026-08-30",
  "times": 12,
  "dayOfWeek": "MONDAY",
  "startTime": "19:20:00",
  "endTime": "20:00:00",
  "period": 1,
  "description": "과목 설명"
}
```

`teacherId`는 선택 값입니다. `teacherId`가 있으면 `teacherAssignedAt`에 현재 시각을 기록하고, 현재 날짜 이후 과목 운영 기간 안에서 Lesson을 자동 생성합니다. 과거 Lesson은 생성하지 않습니다.

## 4. 과목 기본 정보 수정

- **URL**: `/api/v1/subjects/{subjectId}`
- **Method**: `PATCH`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `subject:manage:*`

### Request Body

```json
{
  "name": "국어(수정)",
  "description": "수정된 설명"
}
```

기본 정보 수정은 Lesson에 영향을 주지 않습니다.

## 5. 담당 교사 배정

- **URL**: `/api/v1/subjects/{subjectId}/teacher`
- **Method**: `PATCH`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `subject:manage:*`

### Request Body

```json
{
  "teacherId": 3
}
```

`teacherId`가 있으면 현재 담당 교사를 변경하고 `teacherAssignedAt`을 현재 시각으로 갱신합니다.
미래 Lesson이 있으면 운영 기록이 없는 `SCHEDULED` Lesson의 담당 교사를 변경하고, 미래 Lesson이 없으면 새 Lesson을 생성합니다.

담당 교사를 비우려면 `teacherId`를 `null`로 전달합니다.

```json
{
  "teacherId": null
}
```

이 경우 Subject의 담당 교사와 `teacherAssignedAt`을 비우고, 운영 기록이 없는 미래 `SCHEDULED` Lesson을 soft delete합니다.

## 6. 과목 일정 수정

- **URL**: `/api/v1/subjects/{subjectId}/schedule`
- **Method**: `PATCH`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `subject:manage:*`

### Request Body

```json
{
  "startAt": "2026-06-01",
  "endAt": "2026-08-30",
  "times": 12,
  "dayOfWeek": "TUESDAY",
  "startTime": "20:10:00",
  "endTime": "20:50:00",
  "period": 2
}
```

전달된 필드만 수정합니다.

- `period`, `startTime`, `endTime`만 바뀌면 미래 `SCHEDULED` Lesson의 시간/교시를 수정합니다.
- `dayOfWeek`, `startAt`, `endAt`, `times`가 바뀌면 미래 `SCHEDULED` Lesson을 soft delete한 뒤 새 일정으로 미래 Lesson을 재생성합니다.
- 담당 교사가 없는 과목은 Subject 일정만 수정하고 Lesson은 생성하지 않습니다.

## 7. 과목 삭제

- **URL**: `/api/v1/subjects/{subjectId}`
- **Method**: `DELETE`
- **Auth**: 필요
- **Roles**: `ADMIN` 또는 `subject:manage:*`
- **Description**: 과목을 비활성화합니다.

현재 과목 삭제는 Subject만 soft delete하며 이미 생성된 Lesson은 삭제하지 않습니다.

## 대표 실패 케이스

| 상황 | HTTP Status |
| --- | --- |
| 인증 없이 접근 | `401 Unauthorized` |
| 권한 없는 사용자가 생성/수정/삭제 시도 | `403 Forbidden` |
| 존재하지 않는 과목 또는 분반 | `404 Not Found` |
| 날짜/시간 범위가 유효하지 않음 | `400 Bad Request` |
| 같은 분반에 기간이 겹치는 동일 요일/교시 과목이 있음 | `409 Conflict` |
| 담당 교사로 배정할 수 없는 사용자 | `400 Bad Request` |
| 운영 기록이 있는 미래 Lesson 때문에 자동 변경 불가 | `409 Conflict` |
| 새 담당 교사 또는 새 일정이 기존 수업과 겹침 | `409 Conflict` |
