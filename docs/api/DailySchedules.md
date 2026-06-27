# DailySchedule API

하루 단위 수업 운영 기록, 교사 출석, 학생 출석, 수업 일지, 봉사 시간을 관리합니다.

DailySchedule은 같은 분반과 같은 날짜의 Lesson들을 하루 단위로 묶은 운영 기록입니다. 캘린더에서 교시 단위 일정을 보여줄 때는 Lesson API를 사용하고, 수업 일지 작성 화면처럼 하루 운영 기록이 필요한 경우 DailySchedule API를 사용합니다.

수업 결강 페이지도 DailySchedule을 기준으로 동작합니다. 결석 요청 생성 API는 `lessonId`가 아니라 `dailyScheduleId`를 입력받습니다.

## 권한 정책

| API | 권한 |
|-----|------|
| `GET /api/v1/daily-schedules` | `VOLUNTEER`, `MANAGER`, `ADMIN` |
| `GET /api/v1/daily-schedules/detail` | `VOLUNTEER`, `MANAGER`, `ADMIN` |
| `GET /api/v1/daily-schedules/{dailyScheduleId}` | `VOLUNTEER`, `MANAGER`, `ADMIN` |
| `GET /api/v1/daily-schedules/volunteer-hours` | `VOLUNTEER`, `MANAGER`, `ADMIN` |
| `POST /api/v1/daily-schedules/journal` | 담당 교사, `ADMIN`, `daily-schedule:manage:*` |
| `PATCH /api/v1/daily-schedules/{dailyScheduleId}/journal` | 담당 교사, `ADMIN`, `daily-schedule:manage:*` |
| `DELETE /api/v1/daily-schedules/{dailyScheduleId}/journal` | 담당 교사, `ADMIN`, `daily-schedule:manage:*` |
| `PATCH /api/v1/daily-schedules/{dailyScheduleId}/student-attendances` | 담당 교사, `ADMIN`, `daily-schedule:manage:*` |
| `PATCH /api/v1/daily-schedules/{dailyScheduleId}/teacher-attendance` | 담당 교사, `ADMIN`, `daily-schedule:manage:*` |
| `PATCH /api/v1/daily-schedules/{dailyScheduleId}/teacher-attendance/check-out` | 담당 교사, `ADMIN`, `daily-schedule:manage:*` |
| `PATCH /api/v1/daily-schedules/{dailyScheduleId}/teacher-attendance/adjustment` | `ADMIN`, `daily-schedule:manage:*` |
| `PATCH /api/v1/daily-schedules/{dailyScheduleId}/status` | `ADMIN`, `daily-schedule:manage:*` |

접근 범위:

- `VOLUNTEER`, `MANAGER`, `ADMIN`은 DailySchedule 목록, 상세, 봉사 시간을 조회할 수 있습니다.
- 상세 응답의 담당 교사 연락처와 주민번호 앞자리는 담당 교사 본인, `ADMIN`, `daily-schedule:read:*`, `daily-schedule:manage:*` 권한 보유자에게만 노출됩니다.
- 다른 교사의 봉사 시간을 조회하려면 `ADMIN`, `daily-schedule:read:*`, `daily-schedule:manage:*` 권한이 필요합니다.
- 담당 교사가 아닌 사용자는 `ADMIN` 또는 `daily-schedule:manage:*` 권한이 있어야 수업 일지, 출석, 상태를 변경할 수 있습니다.
- 상태 변경 API는 운영 보정용 관리자 API입니다. 일반 완료 처리는 교사 출석과 수업 일지 작성 완료 시 자동으로 수행됩니다.

## 권한 코드

DailySchedule 도메인은 다음 권한 코드를 사용합니다.

| 권한 코드 | 설명 |
|-----------|------|
| `daily-schedule:read:*` | 모든 하루 일정의 민감 정보와 다른 교사의 봉사 시간 조회 |
| `daily-schedule:manage:*` | 모든 하루 일정의 수업 일지, 출석, 상태 변경 |

## 조회 정책

- `GET /api/v1/daily-schedules`는 수업 일지 목록 화면에서 사용하는 페이지 조회 API입니다.
- 목록에는 작성된 수업 일지만 포함됩니다. 연결된 활성 Lesson note 중 하나라도 작성되어 있으면 작성된 수업 일지로 판단합니다.
- 목록은 수업 날짜 내림차순, ID 내림차순으로 정렬됩니다.
- `keyword`로 분반명, 담당 교사명, 과목명, 수업 일지 내용을 검색할 수 있습니다.
- `mine=true`이면 로그인 사용자가 담당 교사인 수업 일지만 조회합니다.
- `page`, `size`로 페이지를 지정합니다. 기본값은 `page=0`, `size=10`입니다.
- 삭제된 DailySchedule, DailyTeacherAttendance, DailyStudentAttendance는 조회와 집계에서 제외됩니다.
- 상세 조회는 DailySchedule에 연결된 Lesson 목록, 교사 출석, 학생 출석부를 함께 반환합니다.
- 상세 조회는 `dailyScheduleId` 또는 `lessonDate`와 `classroomId` 조합으로 조회할 수 있습니다.
- 목록과 상세 응답은 교환 여부, 결강 여부, 교환 상대 날짜를 함께 반환합니다.

교환·결강 표시 필드:

| 필드 | 타입 | 설명 |
|------|------|------|
| `isExchanged` | boolean | 교환형 또는 대체형 제안 수락으로 담당 교사가 변경된 일정인지 여부 |
| `isAbsent` | boolean | 결석 요청 승인으로 결강 처리된 일정인지 여부 |
| `exchangedLessonDate` | date, nullable | 교환형 일정의 상대 수업 날짜. 대체형 또는 일반 일정이면 `null` |

예시:

```http
GET /api/v1/daily-schedules?keyword=수학&mine=true&page=0&size=8
```

```json
{
  "content": [
    {
      "dailyScheduleId": 1,
      "lessonDate": "2026-06-20",
      "classroomId": 1,
      "classroomName": "장미반",
      "teacherId": 2,
      "teacherName": "홍길동",
      "activityStartTime": "14:00:00",
      "activityEndTime": "16:00:00",
      "volunteerServiceMinutes": 120,
      "status": "SCHEDULED",
      "isExchanged": true,
      "isAbsent": false,
      "exchangedLessonDate": "2026-06-26",
      "teacherAttendanceStatus": "ABSENT",
      "lessonCount": 3,
      "lessons": [
        {
          "lessonId": 11,
          "period": 1,
          "startTime": "14:00:00",
          "endTime": "14:40:00",
          "subjectName": "수학",
          "note": "1교시에는 분수 덧셈을 복습했습니다."
        },
        {
          "lessonId": 12,
          "period": 2,
          "startTime": "14:50:00",
          "endTime": "15:30:00",
          "subjectName": "수학",
          "note": "2교시에는 문장제 풀이를 진행했습니다."
        }
      ]
    }
  ],
  "page": 0,
  "size": 8,
  "totalElements": 1,
  "totalPages": 1
}
```

날짜/분반 기준 상세 조회:

```http
GET /api/v1/daily-schedules/detail?classroomId=1&lessonDate=2026-06-20
```

응답 구조는 `GET /api/v1/daily-schedules/{dailyScheduleId}`와 동일합니다. 해당 날짜와 분반에 연결된 활성 DailySchedule이 없으면 `404 Not Found`를 반환합니다.

## 수업 일지 정책

- 수업 일지는 DailySchedule에 연결된 교시별 Lesson note를 한 번에 작성하거나 수정합니다.
- 최초 작성은 `lessonDate`, `classroomId` 기준으로 DailySchedule을 조회해 처리합니다.
- 수정과 삭제는 `dailyScheduleId` 기준으로 처리합니다.
- 요청에 포함된 `lessonId`는 해당 DailySchedule과 같은 분반, 같은 날짜의 활성 Lesson이어야 합니다.
- 생성/수정 요청에는 해당 DailySchedule에 연결된 모든 활성 Lesson의 `lessonId`와 `note`가 포함되어야 합니다.
- 수업 일지 내용은 공백일 수 없습니다.
- 개인정보 활용 동의가 `true`이면 주민번호 앞자리 6자리를 함께 입력해야 합니다.
- 개인정보 활용 동의가 `false`이면 주민번호 앞자리를 입력할 수 없습니다.
- `CANCELLED` 상태의 DailySchedule에는 수업 일지를 저장할 수 없습니다.
- `COMPLETED` 상태에서도 잘못 작성한 수업 일지는 수정할 수 있습니다.
- 이미 작성된 수업 일지가 있는 DailySchedule에 생성 API를 호출하면 `409 Conflict`를 반환합니다.
- 수업 일지 삭제는 DailySchedule 자체 삭제가 아니라 연결된 Lesson note와 개인정보 입력값을 초기화합니다.
- 수업 일지 삭제 시 교사 출석과 학생 출석부는 유지합니다.
- 수업 일지 삭제 후 같은 `lessonDate`, `classroomId`로 다시 생성할 수 있습니다.
- 삭제 대상이 `COMPLETED` 상태였다면 DailySchedule과 연결된 Lesson 상태를 `SCHEDULED`로 되돌립니다.

최초 작성 요청:

```http
POST /api/v1/daily-schedules/journal
Content-Type: application/json
```

```json
{
  "lessonDate": "2026-06-20",
  "classroomId": 1,
  "personalInfoConsent": true,
  "residentRegistrationNumberPrefix": "900101",
  "lessonJournals": [
    {
      "lessonId": 11,
      "note": "1교시에는 국어 읽기 활동을 진행했습니다."
    },
    {
      "lessonId": 12,
      "note": "2교시에는 받아쓰기 활동을 진행했습니다."
    }
  ]
}
```

수정 요청:

```http
PATCH /api/v1/daily-schedules/1/journal
Content-Type: application/json
```

```json
{
  "personalInfoConsent": true,
  "residentRegistrationNumberPrefix": "900101",
  "lessonJournals": [
    {
      "lessonId": 11,
      "note": "1교시에는 국어 읽기 활동 내용을 수정했습니다."
    },
    {
      "lessonId": 12,
      "note": "2교시에는 받아쓰기 활동 내용을 수정했습니다."
    }
  ]
}
```

삭제 요청:

```http
DELETE /api/v1/daily-schedules/1/journal
```

성공 시 `204 No Content`를 반환합니다.

## 출석 정책

교사 출석 상태:

| 상태 | 설명 |
|------|------|
| `ABSENT` | 결석 또는 아직 출석 처리 전 |
| `PRESENT` | 출석 |
| `LATE` | 지각 |
| `EXCUSED` | 공결 |

학생 출석 상태:

| 상태 | 설명 |
|------|------|
| `ABSENT` | 결석 또는 아직 출석 처리 전 |
| `PRESENT` | 출석 |
| `LATE` | 지각 |

- 교사 출석을 `PRESENT`, `LATE`, `EXCUSED`로 처리할 때 위치 정보를 함께 저장할 수 있습니다.
- 교사 출석을 `ABSENT`로 처리하면 출석 시각과 위치 정보는 저장하지 않습니다.
- 교사 퇴근 처리는 출근 처리 이후에만 가능합니다.
- 교사 퇴근 처리는 DailySchedule에 연결된 모든 활성 Lesson의 수업 일지 note가 작성된 이후에만 가능합니다.
- 교사 계정은 퇴근 처리를 최초 1회만 할 수 있으며, 이미 퇴근 처리된 경우 재처리할 수 없습니다.
- 관리자는 교사 출석 상태, 출근 시간, 퇴근 시간을 보정할 수 있습니다.
- 관리자 보정 시 `ABSENT`, `EXCUSED` 상태에는 출근 시간과 퇴근 시간을 입력할 수 없습니다.
- 퇴근 시간은 출근 시간보다 빠를 수 없습니다.
- 학생 출석 요청에는 같은 학생이 중복으로 들어올 수 없습니다.
- 학생 출석 요청의 학생은 해당 DailySchedule의 분반에 속한 학생이어야 합니다.
- `CANCELLED` 상태의 DailySchedule에는 교사 출석, 교사 퇴근, 학생 출석을 처리할 수 없습니다.

교사 퇴근 처리:

```http
PATCH /api/v1/daily-schedules/1/teacher-attendance/check-out
```

성공 시 현재 서버 시각을 퇴근 시간으로 저장하고 DailySchedule 상세 응답을 반환합니다.

관리자 교사 출석 보정:

```http
PATCH /api/v1/daily-schedules/1/teacher-attendance/adjustment
Content-Type: application/json

{
  "status": "PRESENT",
  "attendedAt": "2026-06-20T14:00:00",
  "checkedOutAt": "2026-06-20T16:00:00"
}
```

관리자 또는 `daily-schedule:manage:*` 권한자가 교사 출석 상태, 출근 시간, 퇴근 시간을 보정합니다. 위치 정보는 보정하지 않습니다. 출석 상태가 `ABSENT` 또는 `EXCUSED`이면 출근 시간과 퇴근 시간을 입력할 수 없으며, 보정 퇴근 시간은 보정 출근 시간보다 빠를 수 없습니다. 보정 후 DailySchedule과 연결된 Lesson의 완료 상태를 다시 계산합니다.

## 상태 정책

DailySchedule 상태는 다음 값을 사용합니다.

| 상태 | 설명 |
|------|------|
| `SCHEDULED` | 예정 또는 아직 운영 기록이 완료되지 않은 하루 일정 |
| `COMPLETED` | 교사 출석과 수업 일지 작성이 완료된 하루 일정 |
| `CANCELLED` | 휴강 처리된 하루 일정 |

자동 완료 조건:

- 교사 출석 상태가 `ABSENT`가 아니어야 합니다.
- DailySchedule에 연결된 모든 활성 Lesson에 수업 일지 note가 작성되어야 합니다.
- 두 조건을 모두 만족하면 DailySchedule은 `COMPLETED`로 변경되고, 같은 날짜/분반의 활성 Lesson도 `COMPLETED`로 연동됩니다.

관리자 상태 변경:

- `PATCH /api/v1/daily-schedules/{dailyScheduleId}/status`는 관리자 보정용 API입니다.
- 하루 일정 전체 상태를 수동으로 보정할 때 사용하는 기준 API입니다.
- `SCHEDULED`로 변경하면 연결된 활성 Lesson은 `SCHEDULED`로 변경됩니다.
- `COMPLETED`로 변경하면 연결된 활성 Lesson은 `COMPLETED`로 변경됩니다.
- `CANCELLED`로 변경하면 연결된 활성 Lesson은 `CANCELED`로 변경됩니다.
- 특정 교시만 예외적으로 보정해야 하는 경우에는 Lesson 상태 변경 API를 사용할 수 있습니다.

## 봉사 시간 정책

- 봉사 시간은 `DailyTeacherAttendance.volunteerServiceMinutes`를 기준으로 집계합니다.
- `COMPLETED` 상태의 DailySchedule만 집계합니다.
- 교사 출석 상태가 `ABSENT`인 기록은 집계하지 않습니다.
- `teacherId`를 입력하지 않으면 본인 기준으로 조회합니다.
- `from`, `to`를 모두 입력하지 않으면 전체 누적 봉사 시간을 조회합니다.
- `from`만 입력하면 해당 날짜 이후, `to`만 입력하면 해당 날짜 이전으로 집계합니다.

예시:

```http
GET /api/v1/daily-schedules/volunteer-hours?from=2026-06-01&to=2026-06-30
```

```json
{
  "teacherId": 2,
  "from": "2026-06-01",
  "to": "2026-06-30",
  "totalVolunteerServiceMinutes": 360,
  "totalVolunteerServiceHours": 6.00
}
```

전체 누적 조회:

```http
GET /api/v1/daily-schedules/volunteer-hours
```

## 동기화 정책

- Lesson 생성, 수정, 삭제 이벤트를 통해 DailySchedule을 동기화합니다.
- 같은 분반과 같은 날짜의 활성 Lesson이 하나 이상 있으면 DailySchedule을 생성하거나 복원합니다.
- 활성 Lesson이 없으면 DailySchedule, 교사 출석, 학생 출석을 soft delete 합니다.
- DailySchedule의 담당 교사는 같은 날짜/분반의 대표 Lesson 담당 교사를 기준으로 갱신됩니다.
- 활동 시작 시간은 연결된 Lesson 중 가장 이른 시작 시간, 활동 종료 시간은 가장 늦은 종료 시간입니다.
- 봉사 인정 시간은 활동 시작 시간과 종료 시간의 차이로 계산합니다.
- 교환형 제안이 수락되면 양쪽 DailySchedule의 `isExchanged`를 `true`로 설정하고 서로의 수업 날짜를 `exchangedLessonDate`에 저장합니다.
- 대체형 제안이 수락되면 요청 DailySchedule의 `isExchanged`만 `true`로 설정하고 `exchangedLessonDate`는 `null`로 유지합니다.
- 결석 요청이 승인되면 대상 DailySchedule의 `isAbsent`를 `true`로 설정하고 교사 출석을 `EXCUSED`로 변경합니다.

## 대표 실패 케이스

| 상황 | HTTP Status |
|------|-------------|
| 인증 없이 보호 API 접근 | `401 Unauthorized` |
| 다른 교사의 민감 정보 또는 봉사 시간 조회 권한 없음 | `403 Forbidden` |
| 담당자가 아닌 사용자가 수업 일지/출석 변경 시도 | `403 Forbidden` |
| 존재하지 않거나 삭제된 DailySchedule 접근 | `404 Not Found` |
| 이미 작성된 수업 일지 생성 시도 | `409 Conflict` |
| DailySchedule에 연결되지 않은 Lesson으로 수업 일지 저장 | `400 Bad Request` |
| 수업 일지 생성/수정 요청에 일부 교시가 누락됨 | `400 Bad Request` |
| DailySchedule에 연결되지 않은 학생으로 출석 처리 | `400 Bad Request` |
| 학생 출석 요청에 같은 학생이 중복됨 | `400 Bad Request` |
| 휴강 상태에서 수업 일지 또는 출석 처리 시도 | `400 Bad Request` |
| 출근 처리 전 교사 퇴근 처리 시도 | `400 Bad Request` |
| 수업 일지 작성 전 교사 퇴근 처리 시도 | `400 Bad Request` |
| 퇴근 시간이 출근 시간보다 빠름 | `400 Bad Request` |
| 이미 퇴근 처리된 교사 출석에 다시 퇴근 처리 시도 | `409 Conflict` |
| 개인정보 동의 여부와 주민번호 앞자리 입력값이 일치하지 않음 | `400 Bad Request` |
