# 손모음 플랫폼 API 명세서

## 1. 개요

### 1.1 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `/api/v1` |
| 인증 방식 | Session-based (Cookie) |
| Content-Type | `application/json` |
| API 문서 | Swagger UI (`/swagger-ui.html`) |

> **Note:** 상세 API 스펙(Request/Response, 파라미터 등)은 Swagger UI를 참조하세요.
> 이 문서는 도메인별 비즈니스 흐름을 시퀀스 다이어그램으로 설명합니다.

### 1.2 참조 문서

- [PRD](./prd.md)
- [기술 명세서](./tech_spec.md)
- [데이터 모델](./data_model.md)

---

## 2. 공통 규칙

### 2.1 응답 형식

```json
// 성공
{ "success": true, "data": { ... }, "error": null }

// 실패
{ "success": false, "data": null, "error": { "code": "ERROR_CODE", "message": "..." } }
```

### 2.2 인증 필요 API

- 대부분의 API는 로그인 세션 필요
- 미인증 시 `401 Unauthorized` 반환
- 권한 부족 시 `403 Forbidden` 반환

---

## 3. 도메인별 시퀀스 다이어그램

### 3.1 인증 (Authentication)

#### 로그인 플로우

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Client as 클라이언트
    participant API as Auth API
    participant Security as Spring Security
    participant DB as Database

    User->>Client: 이메일/비밀번호 입력
    Client->>API: POST /api/v1/auth/login
    API->>Security: 인증 요청
    Security->>DB: 사용자 조회
    DB-->>Security: 사용자 정보
    Security->>Security: 비밀번호 검증
    alt 인증 성공
        Security-->>API: 인증 완료
        API->>API: 세션 생성
        API-->>Client: 200 OK + 사용자 정보
        Client-->>User: 로그인 성공
    else 인증 실패
        Security-->>API: 인증 실패
        API-->>Client: 401 Unauthorized
        Client-->>User: 로그인 실패
    end
```

#### OAuth2 소셜 로그인

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Client as 클라이언트
    participant API as API Server
    participant Google as Google OAuth

    User->>Client: Google 로그인 클릭
    Client->>API: GET /oauth2/authorization/google
    API-->>Client: 302 Redirect to Google
    Client->>Google: 인증 요청
    User->>Google: Google 계정 로그인
    Google-->>Client: Authorization Code
    Client->>API: Callback with Code
    API->>Google: Access Token 요청
    Google-->>API: Access Token + User Info
    API->>API: 사용자 생성 또는 조회
    API->>API: 세션 생성
    API-->>Client: 302 Redirect to App
    Client-->>User: 로그인 완료
```

---

### 3.2 과목/수업 생성 (Subject & Lesson)

#### 과목 생성 시 수업 자동 생성

```mermaid
sequenceDiagram
    actor Admin as 관리자
    participant API as Subject API
    participant SubjectSvc as SubjectService
    participant Event as EventPublisher
    participant LessonHandler as LessonEventHandler
    participant LessonSvc as LessonService
    participant DB as Database

    Admin->>API: POST /api/v1/subjects
    API->>SubjectSvc: createSubject()
    SubjectSvc->>DB: Subject 저장
    DB-->>SubjectSvc: Subject 생성 완료

    SubjectSvc->>Event: publish(SubjectCreatedEvent)
    Event->>LessonHandler: handleSubjectCreated()

    LessonHandler->>LessonSvc: createLessonsFromSubject()

    loop 수업 횟수만큼 반복
        LessonSvc->>LessonSvc: 날짜 계산 (요일 기반)
        LessonSvc->>DB: Lesson 저장
    end

    DB-->>LessonSvc: Lessons 생성 완료
    LessonSvc-->>LessonHandler: 완료

    SubjectSvc-->>API: Subject 반환
    API-->>Admin: 201 Created
```

---

### 3.3 출석 관리 (Attendance)

#### 교사 출석 체크

```mermaid
sequenceDiagram
    actor Teacher as 봉사자
    participant API as Lesson API
    participant LessonSvc as LessonService
    participant DB as Database

    Teacher->>API: PATCH /api/v1/lessons/{id}/attendance
    Note right of Teacher: { "attendance": "PRESENT" }

    API->>LessonSvc: updateTeacherAttendance()
    LessonSvc->>DB: Lesson 조회
    DB-->>LessonSvc: Lesson 정보

    LessonSvc->>LessonSvc: 권한 검증 (본인 수업인지)

    alt 권한 있음
        LessonSvc->>DB: attendance 상태 업데이트
        DB-->>LessonSvc: 업데이트 완료
        LessonSvc-->>API: 성공
        API-->>Teacher: 200 OK
    else 권한 없음
        LessonSvc-->>API: 권한 오류
        API-->>Teacher: 403 Forbidden
    end
```

#### 학생 출석 일괄 체크

```mermaid
sequenceDiagram
    actor Teacher as 봉사자
    participant API as Attendance API
    participant AttendanceSvc as AttendanceService
    participant DB as Database

    Teacher->>API: PATCH /api/v1/lessons/{id}/attendances/bulk
    Note right of Teacher: { "attendances": [...] }

    API->>AttendanceSvc: bulkUpdateAttendance()
    AttendanceSvc->>DB: 해당 수업의 출석 목록 조회
    DB-->>AttendanceSvc: StudentAttendance 목록

    loop 각 학생별
        AttendanceSvc->>AttendanceSvc: 출석 상태 매핑
        AttendanceSvc->>DB: attendance 업데이트
    end

    DB-->>AttendanceSvc: 업데이트 완료
    AttendanceSvc-->>API: 성공
    API-->>Teacher: 200 OK
```

---

### 3.4 결석 요청 (Absence Request)

#### 결석 요청 및 승인 플로우

```mermaid
sequenceDiagram
    actor Teacher as 봉사자
    actor Admin as 관리자
    participant API as Request API
    participant RequestSvc as AbsenceRequestService
    participant Event as EventPublisher
    participant LessonSvc as LessonService
    participant DB as Database

    %% 결석 요청 생성
    Teacher->>API: POST /api/v1/absence-requests
    Note right of Teacher: { lessonId, reason }

    API->>RequestSvc: createAbsenceRequest()
    RequestSvc->>DB: AbsenceRequest 저장 (status: PENDING)
    DB-->>RequestSvc: 저장 완료
    RequestSvc-->>API: 요청 생성 완료
    API-->>Teacher: 201 Created

    %% 관리자 승인
    Admin->>API: PATCH /api/v1/absence-requests/{id}/approve
    Note right of Admin: { status: "APPROVED", note }

    API->>RequestSvc: approveRequest()
    RequestSvc->>DB: AbsenceRequest 업데이트
    RequestSvc->>Event: publish(AbsenceApprovedEvent)

    Event->>LessonSvc: handleAbsenceApproved()
    LessonSvc->>DB: Lesson attendance → EXCUSED
    DB-->>LessonSvc: 업데이트 완료

    RequestSvc-->>API: 승인 완료
    API-->>Admin: 200 OK
```

---

### 3.5 수업 교환 요청 (Lesson Exchange)

#### 수업 교환 요청 플로우

```mermaid
sequenceDiagram
    actor Teacher as 봉사자 A
    actor Admin as 관리자
    participant API as Request API
    participant RequestSvc as ExchangeRequestService
    participant Event as EventPublisher
    participant LessonSvc as LessonService
    participant DB as Database

    %% 교환 요청 생성
    Teacher->>API: POST /api/v1/lesson-exchange-requests
    Note right of Teacher: { lessonId, title, content }

    API->>RequestSvc: createExchangeRequest()
    RequestSvc->>DB: LessonExchangeRequest 저장 (PENDING)
    DB-->>RequestSvc: 저장 완료
    RequestSvc-->>API: 요청 생성 완료
    API-->>Teacher: 201 Created

    %% 관리자 승인 (대상자 지정)
    Admin->>API: PATCH /api/v1/lesson-exchange-requests/{id}/approve
    Note right of Admin: { status: "APPROVED", exchangeWith: teacherBId }

    API->>RequestSvc: approveExchange()
    RequestSvc->>DB: ExchangeRequest 업데이트
    RequestSvc->>Event: publish(ExchangeApprovedEvent)

    Event->>LessonSvc: handleExchangeApproved()
    LessonSvc->>DB: Lesson teacherId 변경
    DB-->>LessonSvc: 업데이트 완료

    RequestSvc-->>API: 승인 완료
    API-->>Admin: 200 OK
```

---

### 3.6 기자재 구입 요청 (Purchase Request)

#### 구입 요청 및 영수증 업로드

```mermaid
sequenceDiagram
    actor Teacher as 봉사자
    actor Admin as 관리자
    participant API as Request API
    participant RequestSvc as PurchaseRequestService
    participant S3 as AWS S3
    participant DB as Database

    %% 구입 요청 생성
    Teacher->>API: POST /api/v1/purchase-requests
    Note right of Teacher: { subjectId, title, content, price }

    API->>RequestSvc: createPurchaseRequest()
    RequestSvc->>DB: PurchaseRequest 저장 (PENDING)
    DB-->>RequestSvc: 저장 완료
    API-->>Teacher: 201 Created

    %% 관리자 승인
    Admin->>API: PATCH /api/v1/purchase-requests/{id}/approve
    API->>RequestSvc: approve()
    RequestSvc->>DB: status → APPROVED
    API-->>Admin: 200 OK

    %% 영수증 업로드
    Teacher->>API: POST /api/v1/purchase-requests/{id}/receipts
    Note right of Teacher: multipart/form-data (file)

    API->>S3: 이미지 업로드
    S3-->>API: imageUrl
    API->>DB: PurchaseReceipt 저장
    DB-->>API: 저장 완료
    API-->>Teacher: 201 Created + imageUrl
```

---

### 3.7 수업 일지 (Lesson Review)

#### 수업 일지 작성

```mermaid
sequenceDiagram
    actor Teacher as 봉사자
    participant API as Lesson API
    participant ReviewSvc as LessonReviewService
    participant DB as Database

    Teacher->>API: PUT /api/v1/lessons/{id}/review
    Note right of Teacher: { content: "수업 내용..." }

    API->>ReviewSvc: createOrUpdateReview()
    ReviewSvc->>DB: LessonReview 조회

    alt 기존 일지 존재
        ReviewSvc->>DB: content 업데이트
    else 신규 작성
        ReviewSvc->>DB: LessonReview 생성
    end

    DB-->>ReviewSvc: 저장 완료
    ReviewSvc-->>API: Review 반환
    API-->>Teacher: 200 OK
```

---

## 4. API 엔드포인트 요약

> 상세 스펙은 Swagger UI (`/swagger-ui.html`) 참조

### 4.1 인증

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/auth/login` | 로그인 |
| POST | `/api/v1/auth/logout` | 로그아웃 |
| GET | `/api/v1/auth/me` | 현재 사용자 조회 |

### 4.2 사용자

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/v1/users` | 목록 조회 (Admin) |
| GET | `/api/v1/users/{id}` | 상세 조회 |
| POST | `/api/v1/users` | 생성 (Admin) |
| PUT | `/api/v1/users/{id}` | 수정 |
| DELETE | `/api/v1/users/{id}` | 삭제 (Admin) |

### 4.3 분반/학생/과목

| Method | Endpoint | 설명 |
|--------|----------|------|
| CRUD | `/api/v1/classrooms/**` | 분반 관리 |
| CRUD | `/api/v1/students/**` | 학생 관리 |
| CRUD | `/api/v1/subjects/**` | 과목 관리 |

### 4.4 수업

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/v1/lessons` | 수업 목록 |
| GET | `/api/v1/lessons/my` | 내 수업 (캘린더) |
| GET | `/api/v1/lessons/{id}` | 수업 상세 |
| PATCH | `/api/v1/lessons/{id}/attendance` | 교사 출석 |
| GET | `/api/v1/lessons/{id}/attendances` | 학생 출석 목록 |
| PATCH | `/api/v1/lessons/{id}/attendances/bulk` | 학생 출석 일괄 |
| PUT | `/api/v1/lessons/{id}/review` | 수업 일지 |

### 4.5 요청

| Method | Endpoint | 설명 |
|--------|----------|------|
| CRUD | `/api/v1/absence-requests/**` | 결석 요청 |
| CRUD | `/api/v1/lesson-exchange-requests/**` | 수업 교환 요청 |
| CRUD | `/api/v1/subject-exchange-requests/**` | 과목 교환 요청 |
| CRUD | `/api/v1/purchase-requests/**` | 기자재 구입 요청 |
| POST | `/api/v1/purchase-requests/{id}/receipts` | 영수증 업로드 |
