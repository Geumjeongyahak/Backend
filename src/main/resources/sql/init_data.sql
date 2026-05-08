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
INSERT INTO subjects (id, class_id, teacher_id, name, start_at, end_at, times, day_of_week, start_time, end_time, period, assigned_from, assigned_to, description)
VALUES
    (1, 1, 2, '한글 기초', '2026-02-01', '2026-06-30', 20, 'WEDNESDAY',  '19:20:00', '20:00:00', 1, '2026-02-01', '2026-06-30', '기초 한글 수업'),
    (2, 2, 3, '수학 기초', '2026-02-01', '2026-06-30', 20, 'WEDNESDAY',  '19:20:00', '20:00:00', 1, '2026-02-01', '2026-06-30', '기초 수학 수업'),
    (3, 8, 2, '스마트폰 활용', '2026-02-01', '2026-06-30', 12, 'SATURDAY','19:20:00', '20:00:00', 1, '2026-02-01', '2026-06-30', '스마트폰 사용법');
ALTER SEQUENCE subjects_id_seq RESTART WITH 4;

-- 9. Lessons
INSERT INTO lessons (id, subject_id, teacher_id, period, date, start_time, end_time, status, teacher_attendance)
VALUES
    (1, 1, 2, 1, '2026-06-10', '19:20:00', '20:00:00', 'SCHEDULED', 'ABSENT'),
    (2, 1, 2, 2, '2026-06-10', '20:10:00', '20:50:00', 'SCHEDULED', 'ABSENT'),
    (3, 1, 2, 3, '2026-06-10', '21:00:00', '21:40:00', 'SCHEDULED', 'ABSENT'),
    (4, 2, 3, 1, '2026-06-17', '19:20:00', '20:00:00', 'SCHEDULED', 'ABSENT'),
    (5, 2, 3, 2, '2026-06-17', '20:10:00', '20:50:00', 'SCHEDULED', 'ABSENT'),
    (6, 2, 3, 3, '2026-06-17', '21:00:00', '21:40:00', 'SCHEDULED', 'ABSENT');
ALTER SEQUENCE lessons_id_seq RESTART WITH 7;

-- 10. Students
INSERT INTO students (id, class_id, name, phone_number, description, status)
VALUES
    (1, 1, '이영희', '010-3333-3333', '기초반', 'ENROLLED'),
    (2, 1, '박민수', '010-4444-4444', '기초반', 'ENROLLED');
ALTER SEQUENCE students_id_seq RESTART WITH 3;

-- 11. Student Attendances
INSERT INTO student_attendances (lesson_id, student_id, status, memo)
VALUES
    (1, 1, 'ABSENT', NULL),
    (1, 2, 'ABSENT', NULL);
