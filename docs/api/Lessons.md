# Lesson API

수업 일정, 교사 출석, 학생 출석, 수업 노트를 관리합니다.

Lesson은 Subject에서 생성되는 개별 수업 단위입니다. DailySchedule 도입 전까지 학생 출석, 교사 출석, 수업 노트는 현행 Lesson 단위 구조를 유지합니다.

## 권한 정책

| API | 권한 |
|-----|------|
| `GET /api/v1/lessons` | 전체 공개 |
| `GET /api/v1/lessons/me` | `VOLUNTEER`, `MANAGER`, `ADMIN`, `lesson:read:*` |
| `GET /api/v1/lessons/{lessonId}` | 담당 교사, `MANAGER`, `ADMIN`, `lesson:read:*` |
| `GET /api/v1/lessons/{lessonId}/student-attendances` | 담당 교사, `MANAGER`, `ADMIN`, `lesson:read:*` |
| `GET /api/v1/lessons/{lessonId}/note` | 담당 교사, `MANAGER`, `ADMIN`, `lesson:read:*` |
| `POST /api/v1/lessons` | `ADMIN`, `lesson:write:*` |
| `PATCH /api/v1/lessons/{lessonId}` | `ADMIN`, `lesson:manage:*` |
| `PATCH /api/v1/lessons/{lessonId}/teacher-attendance` | 담당 교사, `ADMIN`, `lesson:manage:*` |
| `PATCH /api/v1/lessons/{lessonId}/student-attendances` | 담당 교사, `ADMIN`, `lesson:manage:*` |
| `PATCH /api/v1/lessons/{lessonId}/status` | `ADMIN`, `lesson:manage:*` |
| `PUT /api/v1/lessons/{lessonId}/note` | 담당 교사, `ADMIN`, `lesson:manage:*` |
| `DELETE /api/v1/lessons/{lessonId}` | `ADMIN`, `lesson:manage:*` |

접근 범위:

- `GET /api/v1/lessons`는 인증 없이 조회할 수 있으며, 응답에는 `teacherName`, `subjectName`이 포함됩니다.
- `VOLUNTEER`와 `MANAGER`는 본인이 담당 교사인 수업의 출석/노트 변경만 수행할 수 있습니다.
- `MANAGER`, `ADMIN`, `lesson:read:*` 권한 보유자는 모든 수업의 상세, 출석부, 노트를 조회할 수 있습니다.
- `ADMIN`, `lesson:manage:*` 권한 보유자는 모든 수업의 수정, 삭제, 출석/노트 변경, 상태 변경을 수행할 수 있습니다.
- `MANAGER` 역할만으로는 수업 생성/수정/삭제/상태 변경 권한을 갖지 않습니다. 필요한 경우 `lesson:write:*` 또는 `lesson:manage:*` 권한을 별도로 부여합니다.

## 권한 코드

Lesson 도메인은 다음 권한 코드를 사용합니다.

| 권한 코드 | 설명 |
|-----------|------|
| `lesson:read:*` | 모든 수업 상세, 출석부, 노트 조회 |
| `lesson:write:*` | 수업 직접 생성 |
| `lesson:manage:*` | 수업 수정, 삭제, 상태 변경, 출석/노트 변경 |

## 조회 정책

- 목록 조회는 `from`, `to` 기간 필터를 사용합니다.
- 기간은 시작일과 종료일을 모두 포함합니다.
- 날짜 범위는 최대 42일입니다.
- `from`은 `to`보다 이후일 수 없습니다.
- 목록은 수업 날짜 오름차순, 교시 오름차순으로 정렬됩니다.
- 삭제된 수업은 목록, 상세, 내 수업 조회에서 제외됩니다.

캘린더 뷰 사용 방식:

- 주간 뷰는 해당 주의 시작일과 종료일을 `from`, `to`로 전달합니다.
- 월간 뷰는 월간 캘린더 그리드에 노출되는 시작일과 종료일을 `from`, `to`로 전달합니다.
- 월간 캘린더가 앞뒤 달 날짜를 포함해 6주 그리드로 표시되는 경우에도 최대 42일 범위 안에서 한 번에 조회할 수 있습니다.
- 응답은 날짜별로 그룹핑하지 않고 flat list로 반환합니다. 클라이언트는 `date`를 기준으로 캘린더 셀에 배치합니다.

예시:

```http
GET /api/v1/lessons?from=2026-02-01&to=2026-03-14
```

```json
[
  {
    "lessonId": 1,
    "date": "2026-02-03",
    "period": 1,
    "startTime": "19:20:00",
    "endTime": "20:00:00",
    "teacherName": "홍길동",
    "subjectName": "한글 기초"
  }
]
```

## 생성/수정 정책

- 직접 수업 생성은 `ADMIN` 또는 `lesson:write:*` 권한이 필요합니다.
- 수업 수정은 `ADMIN` 또는 `lesson:manage:*` 권한이 필요합니다.
- 수업 생성/수정 시 담당 교사는 `VOLUNTEER`, `MANAGER`, `ADMIN` 역할 사용자만 허용됩니다.
- 시작 시간은 종료 시간보다 빨라야 합니다.
- 동일 교사의 같은 날짜 수업 시간이 겹치면 생성/수정이 실패합니다.
- Subject 기반 자동 Lesson 생성 중 충돌 날짜를 스킵하는 정책은 이번 Lesson API 정리 범위에서 변경하지 않습니다. 해당 정책 개선은 별도 이슈에서 다룹니다.

수업 생성 요청:

```http
POST /api/v1/lessons
Content-Type: application/json
```

```json
{
  "subjectId": 1,
  "teacherId": 2,
  "date": "2026-02-20",
  "startTime": "19:20:00",
  "endTime": "20:00:00",
  "period": 1
}
```

수업 생성 응답:

```http
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "lessonId": 1,
  "date": "2026-02-20",
  "period": 1,
  "startTime": "19:20:00",
  "endTime": "20:00:00",
  "status": "SCHEDULED",
  "teacherAttendance": "ABSENT",
  "teacherName": "홍길동",
  "subjectName": "한글 기초",
  "note": null
}
```

## 상태 정책

Lesson 상태는 다음 값을 사용합니다.

| 상태 | 설명 |
|------|------|
| `SCHEDULED` | 예정 또는 아직 운영 기록이 확정되지 않은 수업 |
| `COMPLETED` | 운영자가 완료 처리한 수업 |
| `CANCELED` | 결강 또는 운영상 이유로 취소 처리한 수업 |

일반 상태 변경 API에서는 다음 전이만 허용합니다.

| 현재 상태 | 변경 상태 | 허용 여부 |
|-----------|-----------|-----------|
| `SCHEDULED` | `COMPLETED` | 허용 |
| `SCHEDULED` | `CANCELED` | 허용 |
| `COMPLETED` | `SCHEDULED` | 금지 |
| `COMPLETED` | `CANCELED` | 금지 |
| `CANCELED` | `SCHEDULED` | 금지 |
| `CANCELED` | `COMPLETED` | 금지 |

같은 상태로 다시 변경하는 요청은 멱등하게 성공 처리합니다.

시간이 지났다는 이유만으로 수업을 자동 완료 처리하지 않습니다. 출석, 노트, 일일 운영 마감 기준의 자동 상태 전이는 향후 DailySchedule 도메인에서 별도로 설계합니다.

## 삭제 정책

- 수업 삭제는 soft delete 방식으로 처리합니다.
- 이미 삭제된 수업에 다시 삭제 요청을 보내면 멱등하게 성공합니다.
- 삭제된 수업은 목록/상세/내 수업 조회에서 제외됩니다.
- 삭제된 수업은 상태, 출석, 노트 변경 대상에서 제외됩니다.
- 삭제된 수업을 대상으로 내부 이벤트가 들어오면 변경하지 않고 로그를 남긴 뒤 무시합니다.

## 출석/노트 정책

- 학생 출석부와 수업 노트는 DailySchedule 도입 전까지 Lesson 단위로 유지합니다.
- 교사는 본인이 담당하는 수업의 교사 출석, 학생 출석, 수업 노트를 처리할 수 있습니다.
- `ADMIN` 또는 `lesson:manage:*` 권한 보유자는 모든 수업의 출석과 노트를 처리할 수 있습니다.
- `lesson:read:*` 권한은 출석부와 노트 조회에는 사용할 수 있지만, 변경에는 사용할 수 없습니다.
- 수업 노트는 공백일 수 없습니다.
- 출석/수업일지의 DailySchedule 이전은 별도 이슈에서 처리합니다.

## 대표 실패 케이스

| 상황 | HTTP Status |
|------|-------------|
| 인증 없이 보호 API 접근 | `401 Unauthorized` |
| 권한 없는 사용자가 생성/수정/삭제/상태 변경 시도 | `403 Forbidden` |
| 담당자가 아닌 교사가 타인 수업 출석/노트 변경 시도 | `404 Not Found` |
| 존재하지 않거나 삭제된 수업 접근 | `404 Not Found` |
| 수업 시간이 유효하지 않음 | `400 Bad Request` |
| 수업 상태 전이가 유효하지 않음 | `400 Bad Request` |
| 같은 교사의 수업 시간이 겹침 | `409 Conflict` |
