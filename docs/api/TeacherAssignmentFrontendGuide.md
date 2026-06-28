# Teacher Assignment Frontend Guide

이 문서는 프론트엔드에서 교원 신청, 승인, 시간표 배정, 사용자 표시를 구현할 때 따라야 하는 백엔드 계약을 정리합니다.

## 핵심 개념

### User.classroomId

`User.classroomId`는 사용자의 기본/소속 분반입니다.

- 교원 승인 또는 첫 시간표 배정 시 사용자의 `classroomId`가 없으면 배정된 시간표의 분반으로 채워집니다.
- 이미 `classroomId`가 있으면 자동으로 바꾸지 않습니다.
- 다른 분반의 시간표를 추가로 맡을 수 있습니다.
- 실제 담당 분반 목록은 `User.classroomId` 하나만 보고 판단하면 안 됩니다.

프론트 표기 기준:

| 필드 | 의미 | 화면 예시 |
|------|------|-----------|
| `user.classroomId`, `user.classroomName` | 기본/소속 분반 | 사용자 기본 정보 |
| `user.teacherAssignments[]` | 실제 담당 중인 시간표 슬롯들 | 교사 담당 수업/분반 목록 |
| `user.teacherAssignmentClassroomNames[]` | 실제 담당 분반명 요약 | 사용자 목록 badge |

### Subject

`Subject`는 과목명이기도 하지만, 교사 배정 관점에서는 시간표 슬롯입니다.

하나의 `Subject`는 다음 정보를 가집니다.

- 분반
- 요일
- 운영 기간
- 교시
- 시작/종료 시간
- 담당 교사

하루에 한 과목만 맡으면 `Subject` 1개를 배정합니다. 하루에 2과목을 맡으면 같은 분반, 요일, 운영 기간의 `Subject` 2개를 한 번에 배정합니다.

### Schedule Assignment

프론트에서 교사 배정 단위는 단일 `classroomId`가 아니라 `subjectIds` 목록입니다.

```ts
type ScheduleSubjectIds = number[];
```

서버는 함께 배정되는 `subjectIds`가 다음 조건을 만족하는지 검증합니다.

- 모두 존재해야 함
- 모두 active여야 함
- 같은 `classroomId`
- 같은 `dayOfWeek`
- 같은 `startAt`, `endAt`
- 같은 `period` 중복 없음
- 시간 충돌 없음

## 화면별 구현

## 1. 교원 신청 폼

신청자는 신청 전에 교사 미배정 시간표 후보를 조회해야 합니다.

```http
GET /api/v1/teacher-applications/available-schedules
```

응답 예시:

```json
[
  {
    "scheduleKey": "6:THURSDAY:2026-09-01:2026-12-31",
    "classroomId": 6,
    "classroomName": "국화반",
    "dayOfWeek": "THURSDAY",
    "startAt": "2026-09-01",
    "endAt": "2026-12-31",
    "startTime": "19:20:00",
    "endTime": "20:50:00",
    "subjectIds": [6, 7],
    "subjects": [
      {
        "subjectId": 6,
        "subjectName": "국어 기초",
        "period": 1,
        "startTime": "19:20:00",
        "endTime": "20:00:00"
      },
      {
        "subjectId": 7,
        "subjectName": "수학 기초",
        "period": 2,
        "startTime": "20:10:00",
        "endTime": "20:50:00"
      }
    ]
  }
]
```

권장 표시:

```text
국화반 / 목요일 / 19:20-20:50 / 국어 기초, 수학 기초
```

신청 제출 API:

```http
POST /api/v1/teacher-applications
```

`preferredSubjectId`에는 사용자가 선택한 시간표 후보의 `subjectIds` 중 하나를 전달합니다.

```json
{
  "birthDate": "1999-03-15",
  "phoneNumber": "010-0000-0000",
  "email": "hong@example.com",
  "address": "부산광역시 금정구",
  "educationAndMajor": "부산대학교 국어국문학과 졸업",
  "preferredSubjectId": 6,
  "motivation": "지원 동기",
  "desiredTeacherImage": "희망하는 교사상",
  "meaningOfSharing": "나눔의 의미"
}
```

## 2. 관리자 교원 신청 승인 폼

승인 폼도 같은 시간표 후보 API를 사용합니다.

```http
GET /api/v1/teacher-applications/available-schedules
```

관리자는 신청자의 희망 시간표를 참고하되, 실제 배정 시간표는 별도로 선택할 수 있습니다.

승인 API:

```http
PATCH /api/v1/admin/teacher-applications/{applicationId}/approve
```

요청 body:

```json
{
  "assignedSubjectIds": [6, 7],
  "teacherStartAt": "2026-09-01",
  "teacherEndAt": "2026-12-31",
  "note": "면접 후 승인"
}
```

주의:

- `classroomId`를 보내면 안 됩니다.
- `assignedSubjectIds`는 필수입니다.
- 하루 2과목 배정이면 해당 후보의 `subjectIds` 전체를 보냅니다.

## 3. 교원 신청 상세/목록 표시

`TeacherApplicationResponse`에는 신청자의 희망 시간표와 실제 배정 결과가 함께 들어옵니다.

배정 전:

```json
{
  "status": "PENDING",
  "preferredSubjectId": 6,
  "preferredSubjectName": "국어 기초",
  "preferredClassroomName": "국화반",
  "preferredDayOfWeek": "THURSDAY",
  "preferredStartTime": "19:20:00",
  "preferredEndTime": "20:00:00",
  "assignedSubjects": [],
  "assignedClassroomId": null,
  "assignedClassroomName": null,
  "assignedDayOfWeek": null,
  "assignedStartTime": null,
  "assignedEndTime": null
}
```

승인 후:

```json
{
  "status": "APPROVED",
  "assignedSubjects": [
    {
      "subjectId": 9,
      "subjectName": "문해 복습",
      "period": 1,
      "startTime": "19:20:00",
      "endTime": "20:00:00"
    },
    {
      "subjectId": 10,
      "subjectName": "생활 수학",
      "period": 2,
      "startTime": "20:10:00",
      "endTime": "20:50:00"
    }
  ],
  "assignedClassroomId": 2,
  "assignedClassroomName": "개나리반",
  "assignedDayOfWeek": "FRIDAY",
  "assignedStartTime": "19:20:00",
  "assignedEndTime": "20:50:00"
}
```

## 4. 사용자 화면

사용자 응답에는 기본 분반과 실제 담당 시간표가 분리되어 있습니다.

```json
{
  "id": 6,
  "name": "최승인",
  "classroomId": 2,
  "classroomName": "개나리반",
  "teacherAssignmentCount": 2,
  "teacherAssignmentClassroomNames": ["개나리반"],
  "teacherAssignments": [
    {
      "subjectId": 9,
      "subjectName": "문해 복습",
      "classroomId": 2,
      "classroomName": "개나리반",
      "dayOfWeek": "FRIDAY",
      "period": 1,
      "startTime": "19:20:00",
      "endTime": "20:00:00",
      "startAt": "2026-09-01",
      "endAt": "2026-12-31"
    }
  ]
}
```

표시 기준:

- 사용자 기본 정보에는 `classroomName`을 표시합니다.
- 교사 담당 영역에는 `teacherAssignments`를 표시합니다.
- 기본 분반과 담당 분반이 다를 수 있습니다.

## 5. 내 담당 시간표 조회

로그인한 교사의 현재 담당 active 시간표는 아래 API로 조회합니다.

```http
GET /api/v1/subjects/me
```

이 API는 `User.classroomId`가 아니라 `Subject.teacherId == 로그인 사용자 ID` 기준입니다.

## 6. 관리자 직접 배정/해제

신청 승인과 별개로 관리자는 교사를 시간표에 직접 배정하거나 해제할 수 있습니다.

배정:

```http
PATCH /api/v1/admin/teacher-schedule-assignments
```

```json
{
  "teacherId": 6,
  "subjectIds": [9, 10],
  "confirmTeacherReplacement": false
}
```

기존 담당 교사가 있는 시간표를 다른 교사로 바꾸려면 `confirmTeacherReplacement`를 `true`로 보내야 합니다. 확인 없이 요청하면 `409 Conflict`가 반환됩니다.

해제:

```http
DELETE /api/v1/admin/teacher-schedule-assignments
```

```json
{
  "subjectIds": [9, 10]
}
```

해제해도 기존 채널 권한은 자동 제거하지 않습니다. 같은 분반의 다른 시간표를 계속 맡을 수 있기 때문입니다.

## 7. DailySchedule과 Lesson

교사 출석의 원본 데이터와 처리는 DailySchedule 레벨입니다.

- `LessonSummaryResponse`, `LessonDetailResponse`에는 같은 날짜/분반 DailySchedule 기준 `teacherAttendance`가 포함됩니다.
- Lesson의 `teacherAttendance`는 `isAttended`, `isCheckedOut` 여부만 제공합니다.
- 교사 출석 상태, 출퇴근 시간, 위치, 봉사 시간은 `DailyScheduleDetailResponse.teacherAttendance`에서 확인합니다.
- Lesson은 교시 단위 수업입니다.
- DailySchedule은 같은 분반, 같은 날짜의 Lesson들을 묶은 하루 운영 단위입니다.

프론트에서 하루 출석 상세/일지를 표시하려면 DailySchedule API를 사용해야 합니다.

## 8. Dev 초기 데이터

프론트 로컬 연동 확인용 dev seed입니다.

| 계정 | 비밀번호 | 용도 |
|------|----------|------|
| `admin@test.com` | `admin1234` | 관리자 |
| `teacher01@test.com` | `teacher01` | 기존 교사, 벚꽃반 월요일 시간표 담당 |
| `teacher02@test.com` | `teacher02` | 기존 교사 |
| `guest01@test.com` | `teacher01` | 신규 신청 테스트용 기본 게스트 |
| `applicant01@test.com` | `teacher01` | PENDING 교원 신청 보유 |
| `approved-teacher01@test.com` | `teacher01` | APPROVED 신청과 실제 배정 시간표 보유 |
| `rejected-applicant01@test.com` | `teacher01` | REJECTED 신청 보유 |
| `direct-assign-edge01@test.com` | `teacher01` | 기본 분반은 있지만 교원 활동 기간은 없는 직접 배정 엣지케이스 |
| `cancelled-applicant01@test.com` | `teacher01` | CANCELLED 교원 신청 보유 |

미배정 신청 가능 시간표:

| 분반 | 요일 | subjectIds |
|------|------|------------|
| 국화반 | THURSDAY | `[6, 7]` |
| 주말 스마트폰반 | SATURDAY | `[8]` |

승인/배정 샘플:

| 교사 | 분반 | 요일 | subjectIds |
|------|------|------|------------|
| `teacher01@test.com` | 벚꽃반 | MONDAY | `[4, 5]` |
| `approved-teacher01@test.com` | 개나리반 | FRIDAY | `[9, 10]` |

운영 데이터:

- 2026-09-04, 2026-09-07, 2026-09-11, 2026-09-14 Lesson/DailySchedule 샘플
- 교사 출석/학생 출석부 샘플
- 이벤트 샘플: 신입 교원 오리엔테이션, 9월 개강 준비 회의, 추석 연휴 휴강 안내

## 9. 흔한 실수

| 실수 | 결과 | 올바른 구현 |
|------|------|-------------|
| 승인 요청에 `classroomId` 전송 | `assignedSubjectIds` 검증 실패로 400 | `assignedSubjectIds` 전송 |
| `User.classroomId`만 보고 담당 분반 판단 | 다른 분반 담당 표시 누락 | `teacherAssignments` 사용 |
| Lesson의 `teacherAttendance`에서 출퇴근 시간 기대 | 여부만 제공됨 | 상세 시간은 DailySchedule 상세 조회 |
| 신청 폼에서 Subject 단일 목록만 표시 | 하루 2과목 배정 맥락 누락 | `available-schedules` 사용 |
| 기존 교사 교체 시 confirm 없이 요청 | 409 Conflict | `confirmTeacherReplacement: true` |
