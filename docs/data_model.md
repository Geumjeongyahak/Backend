# 데이터 모델 설계 명세서

## 1. 개요

### 1.1 목적
- 프로젝트의 데이터 구조와 관계를 정의
- 데이터베이스 설계의 기초 자료 제공
- 개발팀 간 데이터 구조 공유 및 일관성 확보

### 1.2 범위
- 비즈니스 요구사항에 따른 엔티티 정의
- 엔티티 간 관계 설정
- 데이터 유형 및 제약조건 명시

### 1.3 참조 문서
- [PRD (Product Requirements Document)](./prd.md)
- [API 흐름 문서](./api-spec/api_spec.md)
- [도메인별 API 문서](./api/README.md)
- [기술 명세서](./tech_spec.md)
- [에러 코드 문서](./error_codes.md)

## 2. 데이터 모델 아키텍처

### 2.1 엔티티 관계 요약

#### 1:N 관계

| 부모 엔티티 | 자식 엔티티 | FK 필드 | 설명 |
|-------------|-------------|---------|------|
| Users | User Roles | user_id | 사용자별 역할 |
| Classrooms | Subjects | class_id | 분반별 과목 |
| Subjects | Lessons | subject_id | 과목별 수업 |
| Users | Subjects | teacher_id | 봉사자가 담당하는 과목 |
| Users | Lessons | teacher_id | 봉사자가 진행하는 수업 |
| Lessons | Student Attendances | lesson_id | 수업별 학생 출석 |
| Lessons | Lesson Reviews | lesson_id | 수업별 수업 일지 |
| Lessons | Absence Requests | lesson_id | 수업별 결석 요청 |
| Lesson Exchange Requests | Lesson Exchange Proposals | request_id | 교환 요청별 제안 |
| Subjects | Purchase Requests | subject_id | 과목별 기자재 구입 요청 |
| Purchase Requests | Purchase Receipts | purchase_request_id | 구입 요청별 영수증 |
| Users | 각종 요청들 | requested_by | 요청자 |
| Users | 각종 요청들 | approval_by | 승인자 |
| Students | Student Attendances | student_id | 학생별 출석 기록 |
| Files | Post Files | file_id | 게시글 이미지 파일 |
| Files | Post Attachments | file_id | 게시글 첨부 파일 |
| Files | Purchase Request Receipts | file_id | 기자재 구입 영수증 파일 |

#### N:M 관계

| 엔티티 A | 엔티티 B | 조인 테이블 | 설명 |
|----------|----------|-------------|------|
| Students | Lessons | student_enrollments | 학생 수업 등록 |

### 2.2 ERD 다이어그램

```mermaid
erDiagram
    %% 사용자 및 권한 관리
    users ||--o{ user_permissions : "has"

    %% 분반 및 과목
    classrooms ||--o{ subjects : "contains"
    users ||--o{ subjects : "teaches"

    %% 수업
    subjects ||--o{ lessons : "generates"
    users ||--o{ lessons : "conducts"

    %% 학생 관련
    students ||--o{ student_enrollments : "enrolls"
    lessons ||--o{ student_enrollments : "has"
    students ||--o{ student_attendances : "has"
    lessons ||--o{ student_attendances : "tracks"

    %% 수업 일지
    lessons ||--o| lesson_reviews : "has"

    %% 요청들
    lessons ||--o{ absence_requests : "has"
    users ||--o{ lesson_exchange_requests : "requests"
    lesson_exchange_requests ||--o{ lesson_exchange_proposals : "has"
    users ||--o{ lesson_exchange_proposals : "proposes"
    subjects ||--o{ purchase_requests : "has"
    purchase_requests ||--o{ purchase_receipts : "has"
    files ||--o{ post_files : "used as image"
    files ||--o{ post_attachments : "used as attachment"
    files ||--o{ purchase_request_receipts : "used as receipt"

    %% 엔티티 정의
    users {
        bigint id PK
        varchar nickname UK
        varchar name
        varchar primary_email UK
        varchar phone_number
        varchar role
    }

    user_permissions {
        bigint id PK
        bigint user_id FK
        varchar permission_code
    }

    classrooms {
        bigint id PK
        varchar name
        varchar type
    }

    students {
        bigint id PK
        varchar name
        varchar phone_number
    }

    subjects {
        bigint id PK
        bigint class_id FK
        bigint teacher_id FK
        varchar name
        date start_at
        date end_at
    }

    lessons {
        bigint id PK
        bigint subject_id FK
        bigint teacher_id FK
        date date
        time start_time
        time end_time
        varchar attendance
    }

    student_enrollments {
        bigint id PK
        bigint student_id FK
        bigint lesson_id FK
    }

    student_attendances {
        bigint id PK
        bigint lesson_id FK
        bigint student_id FK
        varchar attendance
    }

    lesson_reviews {
        bigint id PK
        bigint lesson_id FK
        text content
    }

    absence_requests {
        bigint id PK
        bigint lesson_id FK
        bigint requested_by FK
        text reason
        timestamp expires_at
        varchar status
        timestamp approval_at
        bigint approval_by FK
        text note
    }

    lesson_exchange_requests {
        bigint id PK
        date lesson_date
        bigint requested_by FK
        varchar title
        varchar status
        varchar scope
    }

    lesson_exchange_proposals {
        bigint id PK
        bigint request_id FK
        bigint proposed_by FK
        varchar proposal_type
        varchar status
    }

    purchase_requests {
        bigint id PK
        bigint subject_id FK
        bigint requested_by FK
        bigint price
        varchar status
    }

    purchase_receipts {
        bigint id PK
        bigint purchase_request_id FK
        varchar image_url
    }

    files {
        uuid id PK
        varchar storage_key
        varchar bucket
        varchar public_url UK
        varchar original_name
        varchar content_type
        bigint file_size
        varchar ext
        boolean is_deleted
        timestamp deleted_at
    }
```

## 3. 엔티티 정의

### 3.1 사용자 (Users)

금정야학 플랫폼 서비스를 관리 혹은 이용하는 사용자입니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 사용자 고유 ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 사용자 아이디 (로그인용) |
| name | VARCHAR(50) | NOT NULL | 사용자 실명 |
| email | VARCHAR(100) | UNIQUE, NULL | 이메일 주소 |
| gmail | VARCHAR(100) | UNIQUE, NULL | Gmail 주소 (OAuth) |
| password_hash | VARCHAR(512) | NULL | 암호화된 비밀번호 |
| phone_number | VARCHAR(20) | NULL | 전화번호 |
| client_id | VARCHAR(512) | NULL | OAuth Client ID |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

### 3.2 역할 (RoleType)

사용자의 기본 역할은 `users.role` 컬럼에 저장되며, `RoleType` Enum으로 관리됩니다.

**역할 목록:**
- `ADMIN`: 관리자
- `MANAGER`: 매니저
- `VOLUNTEER`: 봉사자
- `GUEST`: 게스트

**Spring Security 권한 매핑:**
- 모든 역할은 `ROLE_` prefix를 가진 권한으로 매핑됩니다.
- 예: `ADMIN` -> `ROLE_ADMIN`, `MANAGER` -> `ROLE_MANAGER`, `VOLUNTEER` -> `ROLE_VOLUNTEER`

### 3.3 사용자 개별 권한 (user_permissions)

역할 외에 사용자별 세부 권한을 관리하는 테이블입니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| user_id | BIGINT | FOREIGN KEY, NOT NULL | 사용자 ID |
| permission_code | VARCHAR(100) | NOT NULL | 권한 코드 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

### 3.4 분반(classrooms)

분반을 관리하는 엔티티입니다. 관리자가 추가 및 수정을 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| name | VARCHAR(50) | NOT NULL | 분반 이름 |
| type | VARCHAR(20) | NOT NULL | 주중반, 주말반 등 |
| description | TEXT | NULL | 추가 정보 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

### 3.5 학생 (students)

학생을 관리하는 엔티티입니다. 현재로서는 관리자가 추가 및 등록을 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| name | VARCHAR(50) | NOT NULL | 학생 이름 |
| phone_number | VARCHAR(20) | NULL | 학생 전화번호 |
| description | TEXT | NULL | 추가 정보 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |


### 3.6 과목(subjects)

학생이 수업할 과목입니다. 학생과 봉사에 대한 협의 후 관리자가 추가 및 수정을 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| class_id | BIGINT | FOREIGN KEY | 분반 ID |
| teacher_id | BIGINT | FOREIGN KEY | 선생님 ID |
| start_at | DATE | NOT NULL | 과목 시작일 |
| end_at | DATE | NOT NULL | 과목 종료일 |
| day_of_week | VARCHAR(20) | NOT NULL | 요일 |
| start_time | TIME | NOT NULL | 시작 시간 |
| end_time | TIME | NOT NULL | 종료 시간 |
| period | INT | NOT NULL | 수업 시간 |
| name | VARCHAR(50) | NOT NULL | 과목 이름 |
| description | TEXT | NULL | 추가 정보 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |


### 3.7 수업 (lessons)

수업을 관리하는 엔티티입니다. 봉사자가 관리자와 협의하여 수업을 생성할 때, 시작일, 종료일, 요일, 시간 등을 기반으로 자동 생성됩니다. 수업이 생성된 후에는 수업에 등록된 학생들에게 자동으로 수업 출석을 생성합니다. 
이 수업은 캘린더 뷰 형태로 일별 / 월별로 조회할 수 있습니다. 

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| subject_id | BIGINT | FOREIGN KEY | 과목 ID |
| teacher_id | BIGINT | FOREIGN KEY | 선생님 ID |
| date | DATE | NOT NULL | 수업 날짜 |
| start_time | TIME | NOT NULL | 수업 시작 시간 |
| end_time | TIME | NOT NULL | 수업 종료 시간 |
| attendance | VARCHAR(20) | NOT NULL | 교사(봉사자)의 출석 여부 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

### 3.8 학생 등록 (student_enrollments)

학생이 수업에 등록하는 엔티티입니다. 수업이 생성된 뒤 학생은 원하는 수업을 등록할 수 있습니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| student_id | BIGINT | FOREIGN KEY | 학생 ID |
| lesson_id | BIGINT | FOREIGN KEY | 수업 ID |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |


### 3.9 학생 출석 (student_attendances)

학생의 출석을 관리하는 엔티티입니다. 수업이 생성된 뒤 등록된 학생에 대해 자동 생성되어 출석을 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| lesson_id | BIGINT | FOREIGN KEY | 수업 ID |
| student_id | BIGINT | FOREIGN KEY | 학생 ID |
| attendance | VARCHAR(20) | NOT NULL | 학생의 출석 여부 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |


### 3.10 수업 일지 (lesson_reviews) 

수업 일지를 관리하는 엔티티입니다. 봉사자가 매 수업이 끝난 뒤 특이사항등을 기재한 수업 일지를 작성합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| lesson_id | BIGINT | FOREIGN KEY | 수업 ID |
| content | TEXT | NOT NULL | 수업 일지 내용 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |


### 3.11 결석 요청 (absence_requests)

교사(봉사자)는 수업에 부득이하게 결석할 때 이를 요청하기 위해 사용하는 엔티티입니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| lesson_id | BIGINT | FOREIGN KEY | 수업 ID |
| requested_by | BIGINT | FOREIGN KEY | 결석 요청자 ID |
| reason | TEXT | NOT NULL | 결석 이유 |
| expires_at | TIMESTAMP | NOT NULL | 결석 요청 만료 시각. 대상 수업일의 00:00으로 자동 설정 |
| status | VARCHAR(20) | NOT NULL | 결석 요청 상태 |
| approval_at | TIMESTAMP | NULL | 결석 요청 승인일시 |
| approval_by | BIGINT | FOREIGN KEY | 결석 요청 승인자 ID |
| note | TEXT | NULL | 추가 정보(관리자가 기입) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**상태 규칙:**
- `PENDING` → `APPROVED`, `REJECTED`, `CANCELLED`, `EXPIRED`
- `APPROVED`, `REJECTED`, `CANCELLED`, `EXPIRED` 상태는 재처리할 수 없음

**정책:**
- 요청자는 대상 수업의 담당 교사여야 함. 관리자/매니저도 담당 교사가 아니면 대리 생성할 수 없음
- 같은 수업과 같은 요청자 기준으로 `PENDING`, `APPROVED` 요청이 있으면 중복 생성 불가
- `REJECTED`, `CANCELLED` 요청은 재요청 가능
- 취소는 물리 삭제가 아니라 `CANCELLED` 상태 변경이며 요청자 본인만 가능
- 승인 시 `AbsenceApprovedEvent`가 발행되어 대상 수업의 교사 출석 상태가 `EXCUSED`로 변경됨
- 만료 시각이 지난 `PENDING` 요청은 스케줄러가 `EXPIRED`로 변경함

### 3.12 수업 교환 요청 (lesson_exchange_requests)

교사(봉사자)는 특정 날짜의 자신의 수업 범위에 대해 수업 교환을 요청할 때 사용하는 엔티티입니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| lesson_date | DATE | NOT NULL | 교환 대상 수업 날짜 |
| requested_by | BIGINT | FOREIGN KEY | 수업 교환 요청자 ID |
| title | VARCHAR(255) | NOT NULL | 수업 교환 요청 제목 |
| classroom_name_snapshot | VARCHAR(255) | NOT NULL | 생성/수정 시점 반 이름 snapshot |
| content | TEXT | NOT NULL | 수업 교환 요청 내용 |
| status | VARCHAR(20) | NOT NULL | 수업 교환 요청 상태 |
| scope | VARCHAR(20) | NOT NULL | 교환 범위 (`FULL`, `PARTIAL`) |
| start_period | INTEGER | NULL | 부분 교환 시작 교시 |
| end_period | INTEGER | NULL | 부분 교환 종료 교시 |
| expires_at | TIMESTAMP | NOT NULL | 제안 가능 만료 시각 |
| processed_at | TIMESTAMP | NULL | 승인/반려 처리 시각 |
| processed_by | BIGINT | FOREIGN KEY | 승인/반려 처리자 ID |
| completed_at | TIMESTAMP | NULL | 제안 수락 완료 시각 |
| cancelled_at | TIMESTAMP | NULL | 요청 취소 시각 |
| rejection_note | TEXT | NULL | 반려 사유 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**상태 규칙:**
- `PENDING` → `APPROVED`, `REJECTED`, `CANCELLED`, `EXPIRED`
- `APPROVED` → `COMPLETED`, `EXPIRED`

### 3.12.1 수업 교환 제안 (lesson_exchange_proposals)

승인된 수업 교환 요청에 대해 다른 봉사자가 교환형 또는 대체형 제안을 등록할 때 사용하는 엔티티입니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| request_id | BIGINT | FOREIGN KEY, NOT NULL | 대상 수업 교환 요청 ID |
| proposed_by | BIGINT | FOREIGN KEY, NOT NULL | 제안자 ID |
| proposal_type | VARCHAR(20) | NOT NULL | 제안 타입 (`EXCHANGE`, `SUBSTITUTION`) |
| proposal_scope | VARCHAR(20) | NULL | 교환형 제안 범위 (`FULL`, `PARTIAL`) |
| lesson_date | DATE | NULL | 교환형 제안 수업 날짜 |
| start_period | INTEGER | NULL | 교환형 제안 시작 교시 |
| end_period | INTEGER | NULL | 교환형 제안 종료 교시 |
| content | TEXT | NOT NULL | 제안 내용 |
| classroom_name_snapshot | VARCHAR(255) | NULL | 교환형 제안의 반 이름 snapshot |
| status | VARCHAR(20) | NOT NULL | 제안 상태 |
| accepted_at | TIMESTAMP | NULL | 제안 수락 시각 |
| withdrawn_at | TIMESTAMP | NULL | 제안 철회 시각 |
| closed_at | TIMESTAMP | NULL | 제안 종료 시각 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**상태 규칙:**
- `ACTIVE` → `WITHDRAWN`, `ACCEPTED`, `CLOSED`

### 3.13 기자재 구입 요청 (purchase_requests)

봉사자 혹은 관리자가 수업에 필요한 기자재를 구입하기 위해 사용하는 엔티티입니다. 

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| subject_id | BIGINT | FOREIGN KEY | 과목 ID |
| requested_by | BIGINT | FOREIGN KEY | 기자재 구입 요청자 ID |
| title | VARCHAR(255) | NOT NULL | 기자재 구입 요청 제목 |
| content | TEXT | NOT NULL | 기자재 구입 요청 내용 |
| price | BIGINT | NOT NULL | 기자재 구입 요청 가격 |
| status | VARCHAR(20) | NOT NULL | 기자재 구입 요청 상태 |
| approval_at | TIMESTAMP | NULL | 기자재 구입 요청 승인일시 |
| approval_by | BIGINT | FOREIGN KEY | 기자재 구입 요청 승인자 ID |
| note | TEXT | NULL | 추가 정보(관리자가 기입) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

### 3.14 기자재 영수증 (purchase_receipts)

봉사자 혹은 관리자가 수업에 필요한 기자재를 구입한 후 이를 영수증으로 기록하기 위해 사용하는 엔티티입니다. 물품 구입 후 영수증을 이미지로 첨부하여 기록합니다. 이는 s3에 저장됩니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 엔티티 고유 ID |
| purchase_request_id | BIGINT | FOREIGN KEY | 기자재 구입 요청 ID |
| image_url | VARCHAR(255) | NOT NULL | 영수증 이미지 URL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

### 3.15 파일 메타데이터 (files)

업로드된 이미지와 첨부파일의 저장소 위치 및 메타데이터를 관리하는 엔티티입니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | UUID | PRIMARY KEY | 파일 고유 ID |
| storage_key | VARCHAR(500) | NOT NULL | 스토리지 내부 객체 경로 |
| bucket | VARCHAR(100) | NOT NULL | 스토리지 버킷 이름 |
| public_url | VARCHAR(1000) | UNIQUE, NOT NULL | 공개 접근 URL |
| original_name | VARCHAR(255) | NULL | 원본 파일명 |
| content_type | VARCHAR(100) | NOT NULL | MIME 타입 |
| file_size | BIGINT | NULL | 파일 크기 |
| ext | VARCHAR(20) | NOT NULL | 파일 확장자 |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Soft delete 여부 |
| deleted_at | TIMESTAMP | NULL | Soft delete 처리 시각 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**정책:**
- 파일 삭제 요청 시 즉시 DB 레코드를 제거하지 않고 `is_deleted = true`, `deleted_at = now()`로 표시합니다.
- 파일 정리 스케줄러는 보관 기간이 지난 soft deleted 파일을 스토리지와 DB에서 최종 삭제합니다.
