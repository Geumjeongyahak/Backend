# Lesson API

수업 일정, 교사 출석, 학생 출석, 수업 노트를 관리합니다.

## 권한 정책

| API | 권한 |
|-----|------|
| `GET /api/v1/lessons` | 인증 사용자 |
| `GET /api/v1/lessons/me` | `VOLUNTEER`, `MANAGER`, `ADMIN` |
| `GET /api/v1/lessons/{lessonId}` | 담당 봉사자, `MANAGER`, `ADMIN` |
| `GET /api/v1/lessons/{lessonId}/student-attendances` | 담당 봉사자, `MANAGER`, `ADMIN` |
| `GET /api/v1/lessons/{lessonId}/note` | 담당 봉사자, `MANAGER`, `ADMIN` |
| `POST /api/v1/lessons` | `MANAGER`, `ADMIN` |
| `PATCH /api/v1/lessons/{lessonId}` | `MANAGER`, `ADMIN` |
| `PATCH /api/v1/lessons/{lessonId}/teacher-attendance` | 담당 봉사자, `MANAGER`, `ADMIN` |
| `PATCH /api/v1/lessons/{lessonId}/student-attendances` | 담당 봉사자, `MANAGER`, `ADMIN` |
| `PATCH /api/v1/lessons/{lessonId}/status` | 담당 봉사자, `MANAGER`, `ADMIN` |
| `PUT /api/v1/lessons/{lessonId}/note` | 담당 봉사자, `MANAGER`, `ADMIN` |
| `DELETE /api/v1/lessons/{lessonId}` | `MANAGER`, `ADMIN` |

`MANAGER`와 `ADMIN`은 모든 수업에 접근할 수 있습니다. `VOLUNTEER`는 본인이 담당하는 수업에 대해서만 상세 조회, 출석 처리, 상태 변경, 노트 조회/수정이 가능합니다.

## 주요 규칙

- 수업 생성/수정/삭제는 운영 관리 작업이므로 `MANAGER` 이상만 수행합니다.
- 수업 생성/수정 시 담당 교사는 `VOLUNTEER` 역할 사용자여야 합니다.
- 동일 교사의 같은 날짜 수업 시간이 겹치면 생성/수정이 실패합니다.
- 수업 삭제는 소프트 삭제로 처리합니다.

## 대표 실패 케이스

| 상황 | HTTP Status |
|------|-------------|
| 인증 없이 접근 | `401 Unauthorized` |
| 권한 없는 사용자가 생성/수정/삭제 시도 | `403 Forbidden` |
| 담당자가 아닌 봉사자가 타인 수업 상세/출석/노트 접근 | `404 Not Found` |
| 존재하지 않는 수업 | `404 Not Found` |
| 수업 시간이 유효하지 않음 | `400 Bad Request` |
| 같은 교사의 수업 시간이 겹침 | `409 Conflict` |
