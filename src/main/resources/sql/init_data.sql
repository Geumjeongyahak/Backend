-- username : admin1234, password : admin1234
INSERT INTO users (id, username, name, password_hash) VALUES
    (1, 'admin1234', 'Administrator', '$2a$10$A0Av/dPBUz5uoDmp0Z/2S.dsMzOWFL5gLK7CrXmQp6Rw2vqWulapi');

-- username : teacher01, password : teacher01
INSERT INTO users (id, username, name, password_hash) VALUES
    (2, 'teacher01', '홍길동', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu');

-- username : teacher02, password : teacher02
INSERT INTO users (id, username, name, password_hash) VALUES
    (3, 'teacher02', '김철수', '$2a$12$jNEpPdWPB8WX6kOR/t9cru3Lz7WwZRw3KHfgoRJBg0ddWUFnymr/O');

INSERT INTO roles (id, name, description) VALUES
    (1, 'ROLE_ADMIN', '기본 권한, 모든 권한을 가진 관리자'),
    (2, 'ROLE_MANAGER', '기본 권한, 일부 관리 권한을 가진 매니저'),
    (3, 'ROLE_VOLUNTEER', '기본 권한, 제한된 권한을 가진 기관의 자원 봉사자'),
    (4, 'ROLE_GUEST', '기본 권한, 읽기 전용 접근 권한을 가진 손님'),
    (1001, 'DEPT_ADMIN', '부서 권한, 총무 부서의 관리 권한'),
    (1002, 'DEPT_HR', '부서 권한, 인사 부서의 관리 권한'),
    (1003, 'DEPT_FINANCE', '부서 권한, 재무 부서의 관리 권한'),
    (1004, 'DEPT_IT', '부서 권한, IT 부서의 관리 권한'),
    (1005, 'DEPT_MARKETING', '부서 권한, 마케팅 부서의 관리 권한'),
    (1006, 'DEPT_ACADEMIC', '부서 권한, 학사 부서의 관리 권한'),
    (2001, 'TEACHER', '부가 권한, 교사 직책의 권한');

INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 1);  -- Assign ADMIN role to the admin user

INSERT INTO user_roles (user_id, role_id) VALUES
    (2, 3);  -- teacher01 유저에게 ROLE_VOLUNTEER 권한 부여

INSERT INTO user_roles (user_id, role_id) VALUES
    (3, 3);  -- teacher02 유저에게 ROLE_VOLUNTEER 권한 부여

INSERT INTO classrooms (id, name, type, description)
VALUES
    (1, '벚꽃반', 'WEEKDAY', '기초 한글'),
    (2, '장미반', 'WEEKDAY', '초등 저학년 수준'),
    (3, '스마트폰반', 'WEEKEND', '스마트폰 기능/앱 사용');

INSERT INTO subjects
(id, class_id, teacher_id, name, start_at, end_at, times, day_of_week, start_time, end_time, period, description)
VALUES
    (1, 1, 2, '한글 기초', '2026-02-01', '2026-06-30', 20, 'FRIDAY',  '19:20:00', '20:00:00', 1, '기초 한글 수업'),
    (2, 2, 3, '수학 기초', '2026-02-01', '2026-06-30', 20, 'FRIDAY',  '20:10:00', '20:50:00', 2, '기초 수학 수업'),
    (3, 3, 2, '스마트폰 활용', '2026-02-01', '2026-06-30', 12, 'SATURDAY','19:20:00', '20:00:00', 1, '스마트폰 사용법');

INSERT INTO lessons
(id, subject_id, teacher_id, period, date, start_time, end_time, status, teacher_attendance)
VALUES
    -- 2026-02-13(금) 벚꽃반 1교시
    (1, 1, 2, 1, '2026-02-13', '19:20:00', '20:00:00', 'SCHEDULED', 'ABSENT'),
    -- 2026-02-13(금) 장미반 2교시
    (2, 2, 3, 2, '2026-02-13', '20:10:00', '20:50:00', 'SCHEDULED', 'ABSENT'),
    -- 2026-02-14(토) 스마트폰반 1교시
    (3, 3, 2, 1, '2026-02-14', '19:20:00', '20:00:00', 'SCHEDULED', 'ABSENT');

INSERT INTO students (id, name, phone_number, description, status)
VALUES
    (1, '이영희', '010-3333-3333', '기초반', 'ENROLLED'),
    (2, '박민수', '010-4444-4444', '기초반', 'ENROLLED');

INSERT INTO student_attendances (lesson_id, student_id, status, memo)
VALUES
    (1, 1, 'ABSENT', NULL),
    (1, 2, 'ABSENT', NULL);