-- username : admin1234, password : admin1234
INSERT INTO users (id, username, name, password_hash) VALUES
    (1, 'admin1234', 'Administrator', '$2a$10$A0Av/dPBUz5uoDmp0Z/2S.dsMzOWFL5gLK7CrXmQp6Rw2vqWulapi');

-- username : teacher01, password : teacher01
INSERT INTO users (id, username, name, password_hash) VALUES
    (2, 'teacher01', '홍길동', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu');

-- username : teacher02, password : teacher02
INSERT INTO users (id, username, name, password_hash) VALUES
    (3, 'teacher02', '김철수', '$2a$12$jNEpPdWPB8WX6kOR/t9cru3Lz7WwZRw3KHfgoRJBg0ddWUFnymr/O');
ALTER SEQUENCE users_id_seq RESTART WITH 4;

INSERT INTO roles (id, name, description) VALUES
    (1, 'ROLE_ADMIN', '플랫폼 전반의 사용자, 게시판, 수업, 요청 데이터를 포함한 모든 기능에 접근하고 시스템 설정을 변경할 수 있는 최고 관리자 권한'),
    (2, 'ROLE_MANAGER', '운영 실무를 담당하는 매니저 권한으로, 주요 업무 데이터를 관리하고 운영 지원 기능을 수행할 수 있는 기본 관리 권한'),
    (3, 'ROLE_VOLUNTEER', '기관 소속 봉사자 권한으로, 본인에게 허용된 수업·게시판·요청 기능을 이용할 수 있는 기본 실무 권한'),
    (4, 'ROLE_GUEST', '제한된 화면과 정보만 열람할 수 있는 읽기 전용 게스트 권한'),
    (1001, 'DEPT_ACADEMIC_PLANNING', '교무기획부 전용 권한으로, 교육 운영 일정 관리, 교무 정책 정리, 학사 기획 관련 게시판과 업무 기능에 접근할 수 있는 부서 권한'),
    (1002, 'DEPT_EDUCATION_RESEARCH', '교육연구부 전용 권한으로, 교육 프로그램 연구, 수업 개선 자료 관리, 커리큘럼 개발 관련 게시판과 업무 기능에 접근할 수 있는 부서 권한'),
    (1003, 'DEPT_STUDENT_SAFETY', '생활안전부 전용 권한으로, 학생 생활지도, 안전 점검, 생활 지원 관련 게시판과 업무 기능에 접근할 수 있는 부서 권한'),
    (1004, 'DEPT_GENERAL_AFFAIRS', '총무부 전용 권한으로, 행정 지원, 내부 운영, 자산 및 문서 관리 관련 게시판과 업무 기능에 접근할 수 있는 부서 권한'),
    (1005, 'DEPT_PUBLIC_RELATIONS', '홍보부 전용 권한으로, 대외 홍보, 소식 전달, 행사 안내 및 커뮤니케이션 관련 게시판과 업무 기능에 접근할 수 있는 부서 권한'),
    (1006, 'DEPT_EDITORIAL', '편집부 전용 권한으로, 콘텐츠 편집, 문서 제작, 소식지 및 게시물 편집 관련 게시판과 업무 기능에 접근할 수 있는 부서 권한'),
    (2001, 'TEACHER', '교사 직책 권한으로, 담당 과목과 수업 운영, 출석 관리, 교육 활동 수행에 필요한 기능을 사용할 수 있는 교육 역할');

INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 1);  -- Assign ADMIN role to the admin user

INSERT INTO departments (id, name, assigned_role_id, description) VALUES
    (1, '교무기획부', 1001, '기관의 교육 운영 계획 수립, 교무 일정 조정, 학사 운영 정책 기획을 담당하는 부서'),
    (2, '교육연구부', 1002, '교육 프로그램 연구, 수업 품질 개선, 커리큘럼 개발과 자료 분석을 담당하는 부서'),
    (3, '생활안전부', 1003, '학생 생활지도, 안전 관리, 생활 지원 체계 운영을 담당하는 부서'),
    (4, '총무부', 1004, '기관의 행정 운영, 문서 관리, 내부 지원과 총무 업무를 담당하는 부서'),
    (5, '홍보부', 1005, '기관 홍보, 대외 소통, 행사 안내와 홍보 콘텐츠 운영을 담당하는 부서'),
    (6, '편집부', 1006, '소식지, 게시글, 각종 문서와 콘텐츠의 편집 및 제작을 담당하는 부서');
ALTER SEQUENCE departments_id_seq RESTART WITH 7;

INSERT INTO user_roles (user_id, role_id) VALUES
    (2, 3);  -- teacher01 유저에게 ROLE_VOLUNTEER 권한 부여

INSERT INTO user_roles (user_id, role_id) VALUES
    (3, 3);  -- teacher02 유저에게 ROLE_VOLUNTEER 권한 부여

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

INSERT INTO channels (id, name, slug, description, channel_type, ref_id, writer_policy, is_default, is_active, sort_order)
VALUES
    (1, '전체보기', 'all', '전체 게시글을 조회하는 기본 채널', 'ALL', NULL, 'ALL_AUTHENTICATED', TRUE, TRUE, 0),
    (2, '공지사항', 'notice', '기관 전체 공지사항 채널', 'ALL', NULL, 'ADMIN_MANAGER_ONLY', TRUE, TRUE, 1),
    (3, '벚꽃반', 'classroom-1', '벚꽃반 게시판', 'CLASSROOM', 1, 'CLASSROOM_MANAGER_TEACHER_ONLY', TRUE, TRUE, 10),
    (4, '개나리반', 'classroom-2', '개나리반 게시판', 'CLASSROOM', 2, 'CLASSROOM_MANAGER_TEACHER_ONLY', TRUE, TRUE, 11),
    (5, '민들레반', 'classroom-3', '민들레반 게시판', 'CLASSROOM', 3, 'CLASSROOM_MANAGER_TEACHER_ONLY', TRUE, TRUE, 12),
    (6, '동백반', 'classroom-4', '동백반 게시판', 'CLASSROOM', 4, 'CLASSROOM_MANAGER_TEACHER_ONLY', TRUE, TRUE, 13),
    (7, '해바라기반', 'classroom-5', '해바라기반 게시판', 'CLASSROOM', 5, 'CLASSROOM_MANAGER_TEACHER_ONLY', TRUE, TRUE, 14),
    (8, '국화반', 'classroom-6', '국화반 게시판', 'CLASSROOM', 6, 'CLASSROOM_MANAGER_TEACHER_ONLY', TRUE, TRUE, 15),
    (9, '주말 영어반', 'classroom-7', '주말 영어반 게시판', 'CLASSROOM', 7, 'CLASSROOM_MANAGER_TEACHER_ONLY', TRUE, TRUE, 16),
    (10, '주말 스마트폰반', 'classroom-8', '주말 스마트폰반 게시판', 'CLASSROOM', 8, 'CLASSROOM_MANAGER_TEACHER_ONLY', TRUE, TRUE, 17),
    (11, '겨울반', 'classroom-9', '겨울반 게시판', 'CLASSROOM', 9, 'CLASSROOM_MANAGER_TEACHER_ONLY', TRUE, TRUE, 18),
    (12, '교무기획부', 'department-1', '교무기획부 게시판', 'DEPARTMENT', 1, 'DEPARTMENT_MEMBER_OR_ADMIN', TRUE, TRUE, 30),
    (13, '교육연구부', 'department-2', '교육연구부 게시판', 'DEPARTMENT', 2, 'DEPARTMENT_MEMBER_OR_ADMIN', TRUE, TRUE, 31),
    (14, '생활안전부', 'department-3', '생활안전부 게시판', 'DEPARTMENT', 3, 'DEPARTMENT_MEMBER_OR_ADMIN', TRUE, TRUE, 32),
    (15, '총무부', 'department-4', '총무부 게시판', 'DEPARTMENT', 4, 'DEPARTMENT_MEMBER_OR_ADMIN', TRUE, TRUE, 33),
    (16, '홍보부', 'department-5', '홍보부 게시판', 'DEPARTMENT', 5, 'DEPARTMENT_MEMBER_OR_ADMIN', TRUE, TRUE, 34),
    (17, '편집부', 'department-6', '편집부 게시판', 'DEPARTMENT', 6, 'DEPARTMENT_MEMBER_OR_ADMIN', TRUE, TRUE, 35);
ALTER SEQUENCE channels_id_seq RESTART WITH 18;

INSERT INTO subjects
(id, class_id, teacher_id, name, start_at, end_at, times, day_of_week, start_time, end_time, period, description)
VALUES
    (1, 1, 2, '한글 기초', '2026-02-01', '2026-06-30', 20, 'FRIDAY',  '19:20:00', '20:00:00', 1, '기초 한글 수업'),
    (2, 2, 3, '수학 기초', '2026-02-01', '2026-06-30', 20, 'FRIDAY',  '20:10:00', '20:50:00', 2, '기초 수학 수업'),
    (3, 8, 2, '스마트폰 활용', '2026-02-01', '2026-06-30', 12, 'SATURDAY','19:20:00', '20:00:00', 1, '스마트폰 사용법');
ALTER SEQUENCE subjects_id_seq RESTART WITH 4;

INSERT INTO lessons
(id, subject_id, teacher_id, period, date, start_time, end_time, status, teacher_attendance)
VALUES
    -- 2026-02-13(금) 벚꽃반 1교시
    (1, 1, 2, 1, '2026-02-13', '19:20:00', '20:00:00', 'SCHEDULED', 'ABSENT'),
    -- 2026-02-13(금) 개나리반 2교시
    (2, 2, 3, 2, '2026-02-13', '20:10:00', '20:50:00', 'SCHEDULED', 'ABSENT'),
    -- 2026-02-14(토) 주말 스마트폰반 1교시
    (3, 3, 2, 1, '2026-02-14', '19:20:00', '20:00:00', 'SCHEDULED', 'ABSENT');
ALTER SEQUENCE lessons_id_seq RESTART WITH 4;

INSERT INTO students (id, name, phone_number, description, status)
VALUES
    (1, '이영희', '010-3333-3333', '기초반', 'ENROLLED'),
    (2, '박민수', '010-4444-4444', '기초반', 'ENROLLED');
ALTER SEQUENCE students_id_seq RESTART WITH 3;

INSERT INTO student_attendances (lesson_id, student_id, status, memo)
VALUES
    (1, 1, 'ABSENT', NULL),
    (1, 2, 'ABSENT', NULL);
