-- 1. Departments
INSERT INTO departments (id, name, description) VALUES
    (1, '교무기획부', '기관의 교육 운영 계획 수립, 교무 일정 조정, 학사 운영 정책 기획을 담당하는 부서'),
    (2, '교육연구부', '교육 프로그램 연구, 수업 품질 개선, 커리큘럼 개발과 자료 분석을 담당하는 부서'),
    (3, '생활안전부', '학생 생활지도, 안전 관리, 생활 지원 체계 운영을 담당하는 부서'),
    (4, '총무부', '기관의 행정 운영, 문서 관리, 내부 지원과 총무 업무를 담당하는 부서'),
    (5, '홍보부', '기관 홍보, 대외 소통, 행사 안내와 홍보 콘텐츠 운영을 담당하는 부서'),
    (6, '편집부', '소식지, 게시글, 각종 문서와 콘텐츠의 편집 및 제작을 담당하는 부서');
ALTER TABLE departments ALTER COLUMN id RESTART WITH 7;

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

ALTER TABLE users ALTER COLUMN id RESTART WITH 5;

-- 3. User Credentials
INSERT INTO user_credentials (id, user_id, provider, credential_email, password_hash, email_verified) VALUES
    (1, 1, 'LOCAL', 'admin@test.com', '$2a$10$A0Av/dPBUz5uoDmp0Z/2S.dsMzOWFL5gLK7CrXmQp6Rw2vqWulapi', TRUE),
    (2, 2, 'LOCAL', 'teacher01@test.com', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE),
    (3, 3, 'LOCAL', 'teacher02@test.com', '$2a$12$jNEpPdWPB8WX6kOR/t9cru3Lz7WwZRw3KHfgoRJBg0ddWUFnymr/O', TRUE),
    (4, 4, 'LOCAL', 'guest01@test.com', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE);
ALTER TABLE user_credentials ALTER COLUMN id RESTART WITH 5;

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
    (2, '장미반', 'WEEKDAY', '평일 초급 학습반'),
    (3, '스마트폰반', 'WEEKEND', '스마트폰 기능/앱 사용');
ALTER TABLE classrooms ALTER COLUMN id RESTART WITH 4;

-- 7. Channels
INSERT INTO channels (id, name, description, channel_type, management_mode, ref_id, access_level, is_default, is_active)
VALUES
    (1, '전체 공지', '기관 전체 공지사항을 게시하는 기본 채널', 'NOTICE', 'SYSTEM_MANAGED', NULL, 'READ_ONLY', TRUE, TRUE),
    (2, '공지사항', '기관 전체 공지사항 채널', 'NOTICE', 'SYSTEM_MANAGED', NULL, 'READ_WRITE', TRUE, TRUE),
    (3, '벚꽃반', '벚꽃반 게시판', 'CLASSROOM', 'SYSTEM_MANAGED', 1, 'READ_WRITE', TRUE, TRUE),
    (4, '장미반', '장미반 게시판', 'CLASSROOM', 'SYSTEM_MANAGED', 2, 'READ_WRITE', TRUE, TRUE),
    (5, '스마트폰반', '스마트폰반 게시판', 'CLASSROOM', 'SYSTEM_MANAGED', 3, 'READ_WRITE', TRUE, TRUE),
    (6, '교무기획부', '교무기획부 게시판', 'DEPARTMENT', 'SYSTEM_MANAGED', 1, 'READ_WRITE', TRUE, TRUE),
    (7, '교육연구부', '교육연구부 게시판', 'DEPARTMENT', 'SYSTEM_MANAGED', 2, 'READ_WRITE', TRUE, TRUE),
    (8, '생활안전부', '생활안전부 게시판', 'DEPARTMENT', 'SYSTEM_MANAGED', 3, 'READ_WRITE', TRUE, TRUE),
    (9, '총무부', '총무부 게시판', 'DEPARTMENT', 'SYSTEM_MANAGED', 4, 'READ_WRITE', TRUE, TRUE),
    (10, '홍보부', '홍보부 게시판', 'DEPARTMENT', 'SYSTEM_MANAGED', 5, 'READ_WRITE', TRUE, TRUE),
    (11, '편집부', '편집부 게시판', 'DEPARTMENT', 'SYSTEM_MANAGED', 6, 'READ_WRITE', TRUE, TRUE);
ALTER TABLE channels ALTER COLUMN id RESTART WITH 12;

-- 8. Subjects
INSERT INTO subjects (id, class_id, teacher_id, name, start_at, end_at, times, day_of_week, start_time, end_time, period, description)
VALUES
    (1, 1, 2, '한글 기초', '2026-02-01', '2026-06-30', 20, 'FRIDAY',  '19:20:00', '20:00:00', 1, '기초 한글 수업'),
    (2, 2, 3, '수학 기초', '2026-02-01', '2026-06-30', 20, 'FRIDAY',  '20:10:00', '20:50:00', 2, '기초 수학 수업'),
    (3, 3, 2, '스마트폰 활용', '2026-02-01', '2026-06-30', 12, 'SATURDAY','19:20:00', '20:00:00', 1, '스마트폰 사용법');
ALTER TABLE subjects ALTER COLUMN id RESTART WITH 4;

-- 9. Lessons
INSERT INTO lessons (id, subject_id, teacher_id, period, date, start_time, end_time, status, teacher_attendance)
VALUES
    (1, 1, 2, 1, '2026-02-13', '19:20:00', '20:00:00', 'SCHEDULED', 'ABSENT'),
    (2, 2, 3, 2, '2026-02-13', '20:10:00', '20:50:00', 'SCHEDULED', 'ABSENT'),
    (3, 3, 2, 1, '2026-02-14', '19:20:00', '20:00:00', 'SCHEDULED', 'ABSENT');
ALTER TABLE lessons ALTER COLUMN id RESTART WITH 4;

-- 10. Students
INSERT INTO students (id, name, phone_number, description, status)
VALUES
    (1, '이영희', '010-3333-3333', '기초반', 'ENROLLED'),
    (2, '박민수', '010-4444-4444', '기초반', 'ENROLLED');
ALTER TABLE students ALTER COLUMN id RESTART WITH 3;

-- 11. Student Attendances
INSERT INTO student_attendances (lesson_id, student_id, status, memo)
VALUES
    (1, 1, 'ABSENT', NULL),
    (1, 2, 'ABSENT', NULL);
