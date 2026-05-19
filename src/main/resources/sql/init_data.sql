-- Initial data for development and testing

-- 1. Departments
INSERT INTO departments (id, name, description) VALUES
    (1, '교무기획부', '기관의 교육 운영 계획 수립, 교무 일정 조정, 학사 운영 정책 기획을 담당하는 부서'),
    (2, '교육연구부', '교육 프로그램 연구, 수업 품질 개선, 커리큘럼 개발과 자료 분석을 담당하는 부서'),
    (3, '생활안전부', '학생 생활지도, 안전 관리, 생활 지원 체계 운영을 담당하는 부서'),
    (4, '총무부', '기관의 행정 운영, 문서 관리, 내부 지원과 총무 업무를 담당하는 부서'),
    (5, '홍보부', '기관 홍보, 대외 소통, 행사 안내와 홍보 콘텐츠 운영을 담당하는 부서'),
    (6, '편집부', '소식지, 게시글, 각종 문서와 콘텐츠의 편집 및 제작을 담당하는 부서');
ALTER SEQUENCE departments_id_seq RESTART WITH 7;

-- 2. Users
-- admin1234 / admin1234
INSERT INTO users (id, nickname, name, primary_email, role, department_id) VALUES
    (1, 'admin1234', '관리자', 'admin@test.com', 'ADMIN', 4);

-- teacher01 / teacher01
INSERT INTO users (id, nickname, name, primary_email, role, department_id) VALUES
    (2, 'teacher01', '홍길동', 'teacher01@test.com', 'VOLUNTEER', 2);

-- teacher02 / teacher02
INSERT INTO users (id, nickname, name, primary_email, role, department_id) VALUES
    (3, 'teacher02', '김철수', 'teacher02@test.com', 'VOLUNTEER', 2);

-- guest01 / guest01
INSERT INTO users (id, nickname, name, primary_email, role, department_id) VALUES
    (4, 'guest01', '이영희', 'guest01@test.com', 'GUEST', NULL);

ALTER SEQUENCE users_id_seq RESTART WITH 5;

-- 3. User Credentials
INSERT INTO user_credentials (id, user_id, provider, credential_email, password_hash, email_verified) VALUES
    (1, 1, 'LOCAL', 'admin@test.com', '$2a$10$A0Av/dPBUz5uoDmp0Z/2S.dsMzOWFL5gLK7CrXmQp6Rw2vqWulapi', TRUE),
    (2, 2, 'LOCAL', 'teacher01@test.com', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE),
    (3, 3, 'LOCAL', 'teacher02@test.com', '$2a$12$jNEpPdWPB8WX6kOR/t9cru3Lz7WwZRw3KHfgoRJBg0ddWUFnymr/O', TRUE),
    (4, 4, 'LOCAL', 'guest01@test.com', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE);

ALTER SEQUENCE user_credentials_id_seq RESTART WITH 5;

-- 4. Department Permissions
INSERT INTO department_permissions (department_id, permission_code) VALUES
    (1, 'department:read:*'),
    (1, 'department:write:*'),
    (2, 'post:read:*'),
    (2, 'post:write:*'),
    (4, 'file:read:*'),
    (4, 'file:write:*');

-- 5. User Permissions
INSERT INTO user_permissions (user_id, permission_code) VALUES
    (2, 'lesson:write:1');

-- 6. Classrooms
INSERT INTO classrooms (id, name, type, description)
VALUES
    (1, '벚꽃반', 'WEEKDAY', '평일 기초 학습반'),
    (2, '개나리반', 'WEEKDAY', '평일 초급 학습반'),
    (3, '민들레반', 'WEEKDAY', '평일 기초 심화반'),
    (4, '동백반', 'WEEKDAY', '평일 보충 학습반'),
    (5, '해바라기반', 'WEEKDAY', '평일 활동 중심 학습반'),
    (6, '국화반', 'WEEKDAY', '평일 맞춤 학습반'),
    (7, '주말 영어반', 'WEEKEND', '주말 영어 학습반'),
    (8, '주말 스마트폰반', 'WEEKEND', '주말 스마트폰 활용반'),
    (9, '겨울반', 'WEEKDAY', '계절 특강 운영반');
ALTER SEQUENCE classrooms_id_seq RESTART WITH 10;

-- 7. Channels
INSERT INTO channels (id, name, description, channel_type, binding_type, ref_id, access_level, allow_guest_read, is_default, is_active)
VALUES
    (2, '공지사항', '기관 전체 공지사항 채널', 'NOTICE', 'STANDALONE', NULL, 'READ_ONLY', TRUE, TRUE, TRUE),
    (3, '자료실', '교육 자료 및 양식 자료실', 'RESOURCE', 'STANDALONE', NULL, 'READ_ONLY', FALSE, TRUE, TRUE),
    (4, '행사안내', '주요 행사 및 일정 안내', 'EVENT', 'STANDALONE', NULL, 'READ_ONLY', TRUE, TRUE, TRUE),
    (5, '벚꽃반', '벚꽃반 전용 채널', 'CLASSROOM', 'DOMAIN_LINKED', 1, 'READ_WRITE', FALSE, FALSE, TRUE),
    (6, '개나리반', '개나리반 전용 채널', 'CLASSROOM', 'DOMAIN_LINKED', 2, 'READ_WRITE', FALSE, FALSE, TRUE),
    (7, '민들레반', '민들레반 전용 채널', 'CLASSROOM', 'DOMAIN_LINKED', 3, 'READ_WRITE', FALSE, FALSE, TRUE),
    (8, '동백반', '동백반 전용 채널', 'CLASSROOM', 'DOMAIN_LINKED', 4, 'READ_WRITE', FALSE, FALSE, TRUE),
    (9, '해바라기반', '해바라기반 전용 채널', 'CLASSROOM', 'DOMAIN_LINKED', 5, 'READ_WRITE', FALSE, FALSE, TRUE),
    (10, '국화반', '국화반 전용 채널', 'CLASSROOM', 'DOMAIN_LINKED', 6, 'READ_WRITE', FALSE, FALSE, TRUE),
    (11, '주말 영어반', '주말 영어반 전용 채널', 'CLASSROOM', 'DOMAIN_LINKED', 7, 'READ_WRITE', FALSE, FALSE, TRUE),
    (12, '주말 스마트폰반', '주말 스마트폰반 전용 채널', 'CLASSROOM', 'DOMAIN_LINKED', 8, 'READ_WRITE', FALSE, FALSE, TRUE),
    (13, '겨울반', '겨울반 전용 채널', 'CLASSROOM', 'DOMAIN_LINKED', 9, 'READ_WRITE', FALSE, FALSE, TRUE),
    (14, '교무기획부', '교무기획부 전용 채널', 'DEPARTMENT', 'DOMAIN_LINKED', 1, 'READ_WRITE', FALSE, FALSE, TRUE),
    (15, '교육연구부', '교육연구부 전용 채널', 'DEPARTMENT', 'DOMAIN_LINKED', 2, 'READ_WRITE', FALSE, FALSE, TRUE),
    (16, '생활안전부', '생활안전부 전용 채널', 'DEPARTMENT', 'DOMAIN_LINKED', 3, 'READ_WRITE', FALSE, FALSE, TRUE),
    (17, '총무부', '총무부 전용 채널', 'DEPARTMENT', 'DOMAIN_LINKED', 4, 'READ_WRITE', FALSE, FALSE, TRUE),
    (18, '홍보부', '홍보부 전용 채널', 'DEPARTMENT', 'DOMAIN_LINKED', 5, 'READ_WRITE', FALSE, FALSE, TRUE),
    (19, '편집부', '편집부 전용 채널', 'DEPARTMENT', 'DOMAIN_LINKED', 6, 'READ_WRITE', FALSE, FALSE, TRUE);
ALTER SEQUENCE channels_id_seq RESTART WITH 20;

-- 8. Subjects
INSERT INTO subjects (id, class_id, teacher_id, name, start_at, end_at, day_of_week, start_time, end_time, period, teacher_assigned_at, description)
VALUES
    (1, 1, 2, '한글 기초', '2026-02-01', '2026-06-30', 'WEDNESDAY',  '19:20:00', '20:00:00', 1, '2026-02-01 00:00:00', '기초 한글 수업'),
    (2, 2, 3, '수학 기초', '2026-02-01', '2026-06-30', 'WEDNESDAY',  '19:20:00', '20:00:00', 1, '2026-02-01 00:00:00', '기초 수학 수업'),
    (3, 8, 2, '스마트폰 활용', '2026-02-01', '2026-06-30', 'SATURDAY','19:20:00', '20:00:00', 1, '2026-02-01 00:00:00', '스마트폰 사용법');
ALTER SEQUENCE subjects_id_seq RESTART WITH 4;

-- 9. Lessons
INSERT INTO lessons (id, subject_id, teacher_id, period, date, start_time, end_time, status)
VALUES
    (1, 1, 2, 1, '2026-06-10', '19:20:00', '20:00:00', 'SCHEDULED'),
    (2, 1, 2, 2, '2026-06-10', '20:10:00', '20:50:00', 'SCHEDULED'),
    (3, 1, 2, 3, '2026-06-10', '21:00:00', '21:40:00', 'SCHEDULED'),
    (4, 2, 3, 1, '2026-06-17', '19:20:00', '20:00:00', 'SCHEDULED'),
    (5, 2, 3, 2, '2026-06-17', '20:10:00', '20:50:00', 'SCHEDULED'),
    (6, 2, 3, 3, '2026-06-17', '21:00:00', '21:40:00', 'SCHEDULED'),
    (7, 1, 2, 1, '2026-05-13', '19:20:00', '20:00:00', 'COMPLETED'),
    (8, 1, 2, 2, '2026-05-13', '20:10:00', '20:50:00', 'COMPLETED'),
    (9, 1, 2, 3, '2026-05-13', '21:00:00', '21:40:00', 'COMPLETED');
UPDATE lessons SET note = '1교시에는 한글 자음과 모음 복습을 진행했습니다.' WHERE id = 7;
UPDATE lessons SET note = '2교시에는 짧은 단어 읽기와 받아쓰기를 연습했습니다.' WHERE id = 8;
UPDATE lessons SET note = '3교시에는 생활 문장 읽기 활동과 개별 피드백을 진행했습니다.' WHERE id = 9;
ALTER SEQUENCE lessons_id_seq RESTART WITH 10;

-- 10. Daily Schedules
INSERT INTO daily_schedules (
    id, classroom_id, teacher_id, lesson_date, activity_start_time, activity_end_time, status, is_deleted,
    created_at, updated_at
)
VALUES
    (1, 1, 2, '2026-06-10', '19:20:00', '21:40:00', 'SCHEDULED', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (2, 2, 3, '2026-06-17', '19:20:00', '21:40:00', 'SCHEDULED', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (3, 1, 2, '2026-06-24', '19:20:00', '21:40:00', 'SCHEDULED', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (4, 2, 3, '2026-06-24', '19:20:00', '21:40:00', 'SCHEDULED', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (5, 8, 2, '2026-06-27', '19:20:00', '20:00:00', 'SCHEDULED', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (6, 1, 2, '2026-05-13', '19:20:00', '21:40:00', 'COMPLETED', FALSE, '2026-05-13 18:00:00', '2026-05-13 22:00:00');
UPDATE daily_schedules
SET resident_registration_number_prefix = '900101',
    personal_info_consent = TRUE
WHERE id = 6;
ALTER SEQUENCE daily_schedules_id_seq RESTART WITH 7;

-- 10-1. Daily Teacher Attendances
INSERT INTO daily_teacher_attendances (
    id, daily_schedule_id, status, volunteer_service_minutes, attended_at, latitude, longitude, is_deleted,
    created_at, updated_at
)
VALUES
    (1, 6, 'PRESENT', 140, '2026-05-13 19:18:00', NULL, NULL, FALSE, '2026-05-13 19:18:00', '2026-05-13 19:18:00');
ALTER SEQUENCE daily_teacher_attendances_id_seq RESTART WITH 2;

-- 11. Absence Requests
INSERT INTO absence_requests (
    id, daily_schedule_id, requested_by, title, reason, expires_at, status,
    approval_at, approval_by, note, created_at, updated_at
)
VALUES
    (1, 3, 2, '6월 24일 한글 기초 결석 요청', '개인 일정으로 6월 24일 한글 기초 수업에 참석하기 어렵습니다.',
     '2026-06-24 00:00:00', 'PENDING', NULL, NULL, NULL, '2026-05-26 09:00:00', '2026-05-26 09:00:00'),
    (2, 4, 3, '승인된 수학 기초 결석 요청', '병원 진료 일정으로 수업 참석이 어려워 결석을 요청했습니다.',
     '2026-06-24 00:00:00', 'APPROVED', '2026-05-27 10:30:00', 1, NULL, '2026-05-26 13:20:00', '2026-05-27 10:30:00'),
    (3, 5, 2, '반려된 스마트폰 활용 결석 요청', '일정 조정 가능성이 있어 결석 요청을 제출했습니다.',
     '2026-06-27 00:00:00', 'REJECTED', '2026-05-28 14:10:00', 1, '수업 대체 운영 계획 확인 후 다시 요청해주세요.', '2026-05-27 16:40:00', '2026-05-28 14:10:00');
ALTER SEQUENCE absence_requests_id_seq RESTART WITH 4;

-- 12. Lesson Exchange Requests
INSERT INTO lesson_exchange_requests (
    id, daily_schedule_id, lesson_date, requested_by, title, classroom_name_snapshot, content, status,
    expires_at, processed_at, processed_by, completed_at, cancelled_at, rejection_note,
    created_at, updated_at
)
VALUES
    (1, 1, '2026-06-10', 2, '6월 10일 한글 기초 수업 교환 요청', '벚꽃반', '개인 일정으로 6월 10일 한글 기초 수업 교환을 요청합니다.', 'PENDING',
     '2026-06-07 23:59:00', NULL, NULL, NULL, NULL, NULL, '2026-05-20 10:00:00', '2026-05-20 10:00:00'),
    (2, 2, '2026-06-17', 3, '6월 17일 수학 기초 수업 교환 요청', '개나리반', '해당 날짜 수학 기초 수업을 다른 선생님과 교환하고 싶습니다.', 'PENDING',
     '2026-06-14 23:59:00', NULL, NULL, NULL, NULL, NULL, '2026-05-21 09:30:00', '2026-05-21 09:30:00'),
    (3, 1, '2026-06-10', 2, '승인된 한글 기초 수업 교환 요청', '벚꽃반', '관리자가 승인해 제안을 받을 수 있는 수업 교환 요청입니다.', 'APPROVED',
     '2026-06-07 23:59:00', '2026-05-22 11:00:00', 1, NULL, NULL, NULL, '2026-05-21 13:10:00', '2026-05-22 11:00:00'),
    (4, 2, '2026-06-17', 3, '반려된 수학 기초 수업 교환 요청', '개나리반', '일정상 교환 요청을 제출했으나 반려된 예시입니다.', 'REJECTED',
     '2026-06-14 23:59:00', '2026-05-23 14:20:00', 1, NULL, NULL, '대체 수업 일정이 충분하지 않습니다.', '2026-05-22 15:00:00', '2026-05-23 14:20:00'),
    (5, 1, '2026-06-10', 2, '완료된 한글 기초 수업 교환 요청', '벚꽃반', '제안 수락까지 완료된 수업 교환 요청입니다.', 'COMPLETED',
     '2026-06-07 23:59:00', '2026-05-24 10:00:00', 1, '2026-05-25 16:00:00', NULL, NULL, '2026-05-23 08:40:00', '2026-05-25 16:00:00'),
    (6, 2, '2026-06-17', 3, '만료된 수학 기초 수업 교환 요청', '개나리반', '만료 처리된 수업 교환 요청입니다.', 'EXPIRED',
     '2026-05-24 23:59:00', NULL, NULL, NULL, NULL, NULL, '2026-05-20 16:20:00', '2026-05-25 00:10:00'),
    (7, 1, '2026-06-10', 2, '취소된 한글 기초 수업 교환 요청', '벚꽃반', '요청자가 직접 취소한 수업 교환 요청입니다.', 'CANCELLED',
     '2026-06-07 23:59:00', NULL, NULL, NULL, '2026-05-24 18:00:00', NULL, '2026-05-24 12:00:00', '2026-05-24 18:00:00');
ALTER SEQUENCE lesson_exchange_requests_id_seq RESTART WITH 8;

-- 13. Lesson Exchange Proposals
INSERT INTO lesson_exchange_proposals (
    id, request_id, proposed_by, proposal_type, daily_schedule_id, lesson_date, content, classroom_name_snapshot,
    status, accepted_at, withdrawn_at, closed_at, created_at, updated_at
)
VALUES
    (1, 3, 3, 'EXCHANGE', 2, '2026-06-17', '6월 17일 수학 기초 수업과 교환 가능합니다.', '개나리반',
     'ACTIVE', NULL, NULL, NULL, '2026-05-22 13:00:00', '2026-05-22 13:00:00'),
    (2, 3, 3, 'SUBSTITUTION', NULL, NULL, '교환 대신 6월 10일 한글 기초 수업을 대체할 수 있습니다.', '개나리반',
     'ACTIVE', NULL, NULL, NULL, '2026-05-23 08:30:00', '2026-05-23 08:30:00'),
    (3, 3, 3, 'EXCHANGE', 2, '2026-06-17', '한 번 제안했다가 철회한 교환 제안입니다.', '개나리반',
     'WITHDRAWN', NULL, '2026-05-24 09:00:00', NULL, '2026-05-23 12:40:00', '2026-05-24 09:00:00'),
    (4, 5, 3, 'EXCHANGE', 2, '2026-06-17', '완료된 요청에서 수락된 교환 제안입니다.', '개나리반',
     'ACCEPTED', '2026-05-25 16:00:00', NULL, NULL, '2026-05-24 09:00:00', '2026-05-25 16:00:00'),
    (5, 5, 3, 'SUBSTITUTION', NULL, NULL, '다른 제안이 수락되어 종료된 대체 제안입니다.', '개나리반',
     'CLOSED', NULL, NULL, '2026-05-25 16:00:00', '2026-05-24 10:30:00', '2026-05-25 16:00:00');
ALTER SEQUENCE lesson_exchange_proposals_id_seq RESTART WITH 6;

-- 14. Students
INSERT INTO students (id, class_id, name, phone_number, description, status)
VALUES
    (1, 1, '이영희', '010-3333-3333', '기초반', 'ENROLLED'),
    (2, 1, '박민수', '010-4444-4444', '기초반', 'ENROLLED');
ALTER SEQUENCE students_id_seq RESTART WITH 3;

-- 15. Daily Student Attendances
INSERT INTO daily_student_attendances (
    id, daily_schedule_id, student_id, status, is_deleted, created_at, updated_at
)
VALUES
    (1, 6, 1, 'PRESENT', FALSE, '2026-05-13 22:00:00', '2026-05-13 22:00:00'),
    (2, 6, 2, 'LATE', FALSE, '2026-05-13 22:00:00', '2026-05-13 22:00:00');
ALTER SEQUENCE daily_student_attendances_id_seq RESTART WITH 3;

