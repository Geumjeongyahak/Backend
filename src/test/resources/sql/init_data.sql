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
INSERT INTO users (id, name, primary_email, role, department_id) VALUES
    (1, '관리자', 'admin@test.com', 'ADMIN', 4);

-- teacher01 / teacher01
INSERT INTO users (id, name, primary_email, role, department_id) VALUES
    (2, '홍길동', 'teacher01@test.com', 'VOLUNTEER', 2);

-- teacher02 / teacher02
INSERT INTO users (id, name, primary_email, role, department_id) VALUES
    (3, '김철수', 'teacher02@test.com', 'VOLUNTEER', 2);

-- guest01 / guest01
INSERT INTO users (id, name, primary_email, role, department_id) VALUES
    (4, '이영희', 'guest01@test.com', 'GUEST', NULL);

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
    (1, 'department:write:*'),
    (1, 'department:manage:*'),
    (2, 'channel:read:*'),
    (2, 'channel:write:*'),
    (4, 'purchase-request:read:*'),
    (4, 'purchase-request:review:*');

-- 5. User Permissions
INSERT INTO user_permissions (user_id, permission_code) VALUES
    (2, 'channel:write:1');

-- 6. Classrooms
INSERT INTO classrooms (id, name, type, description)
VALUES
    (1, '벚꽃반', 'WEEKDAY', '평일 기초 학습반'),
    (2, '장미반', 'WEEKDAY', '평일 초급 학습반'),
    (3, '스마트폰반', 'WEEKEND', '스마트폰 기능/앱 사용');
ALTER TABLE classrooms ALTER COLUMN id RESTART WITH 4;

-- 7. Channels
INSERT INTO channels (id, name, description, channel_type, binding_type, ref_id, access_level, allow_guest_read, is_default, is_active)
VALUES
    (1, '공지사항', '기관 전체 공지사항 채널', 'NOTICE', 'STANDALONE', NULL, 'READ_ONLY', TRUE, TRUE, TRUE),
    (2, '이벤트', '기관 전체 이벤트 채널', 'EVENT', 'STANDALONE', NULL, 'READ_ONLY', TRUE, TRUE, TRUE),
    (3, '자료실', '기관 공용 자료실 채널', 'RESOURCE', 'STANDALONE', NULL, 'READ_ONLY', FALSE, TRUE, TRUE),
    (4, '벚꽃반', '벚꽃반 게시판', 'CLASSROOM', 'DOMAIN_LINKED', 1, 'READ_WRITE', FALSE, TRUE, TRUE),
    (5, '장미반', '장미반 게시판', 'CLASSROOM', 'DOMAIN_LINKED', 2, 'READ_WRITE', FALSE, TRUE, TRUE),
    (6, '스마트폰반', '스마트폰반 게시판', 'CLASSROOM', 'DOMAIN_LINKED', 3, 'READ_WRITE', FALSE, TRUE, TRUE),
    (7, '교무기획부', '교무기획부 게시판', 'DEPARTMENT', 'DOMAIN_LINKED', 1, 'READ_WRITE', FALSE, TRUE, TRUE),
    (8, '교육연구부', '교육연구부 게시판', 'DEPARTMENT', 'DOMAIN_LINKED', 2, 'READ_WRITE', FALSE, TRUE, TRUE),
    (9, '생활안전부', '생활안전부 게시판', 'DEPARTMENT', 'DOMAIN_LINKED', 3, 'READ_WRITE', FALSE, TRUE, TRUE),
    (10, '총무부', '총무부 게시판', 'DEPARTMENT', 'DOMAIN_LINKED', 4, 'READ_WRITE', FALSE, TRUE, TRUE),
    (11, '홍보부', '홍보부 게시판', 'DEPARTMENT', 'DOMAIN_LINKED', 5, 'READ_WRITE', FALSE, TRUE, TRUE),
    (12, '편집부', '편집부 게시판', 'DEPARTMENT', 'DOMAIN_LINKED', 6, 'READ_WRITE', FALSE, TRUE, TRUE);
ALTER TABLE channels ALTER COLUMN id RESTART WITH 13;

-- 8. Subjects
INSERT INTO subjects (id, class_id, teacher_id, name, start_at, end_at, day_of_week, start_time, end_time, period, teacher_assigned_at, description)
VALUES
    (1, 1, 2, '한글 기초', '2026-02-01', '2026-06-30', 'WEDNESDAY',  '19:20:00', '20:00:00', 1, '2026-02-01 00:00:00', '기초 한글 수업'),
    (2, 2, 3, '수학 기초', '2026-02-01', '2026-06-30', 'WEDNESDAY',  '19:20:00', '20:00:00', 1, '2026-02-01 00:00:00', '기초 수학 수업'),
    (3, 3, 2, '스마트폰 활용', '2026-02-01', '2026-06-30', 'SATURDAY','19:20:00', '20:00:00', 1, '2026-02-01 00:00:00', '스마트폰 사용법');
ALTER TABLE subjects ALTER COLUMN id RESTART WITH 4;

-- 9. Lessons
INSERT INTO lessons (id, subject_id, teacher_id, period, date, start_time, end_time, status)
VALUES
    (1, 1, 2, 1, '2026-06-10', '19:20:00', '20:00:00', 'SCHEDULED'),
    (2, 1, 2, 2, '2026-06-10', '20:10:00', '20:50:00', 'SCHEDULED'),
    (3, 1, 2, 3, '2026-06-10', '21:00:00', '21:40:00', 'SCHEDULED'),
    (4, 2, 3, 1, '2026-06-17', '19:20:00', '20:00:00', 'SCHEDULED'),
    (5, 2, 3, 2, '2026-06-17', '20:10:00', '20:50:00', 'SCHEDULED'),
    (6, 2, 3, 3, '2026-06-17', '21:00:00', '21:40:00', 'SCHEDULED');
ALTER TABLE lessons ALTER COLUMN id RESTART WITH 7;

-- 10. Students
INSERT INTO students (id, class_id, name, phone_number, description, status)
VALUES
    (1, 1, '이영희', '010-3333-3333', '기초반', 'ENROLLED'),
    (2, 1, '박민수', '010-4444-4444', '기초반', 'ENROLLED');
ALTER TABLE students ALTER COLUMN id RESTART WITH 3;

