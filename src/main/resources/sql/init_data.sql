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

-- 2. Classrooms (moved before users to satisfy FK constraint)
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

-- 3. Users
-- admin1234 / admin1234
INSERT INTO users (id, name, primary_email, role, department_id, classroom_id, resident_registration_number_prefix, teacher_start_at, teacher_end_at) VALUES
    (1, '관리자', 'admin@test.com', 'ADMIN', 4, NULL, NULL, NULL, NULL);

-- teacher01 / teacher01
INSERT INTO users (id, name, primary_email, role, department_id, classroom_id, resident_registration_number_prefix, teacher_start_at, teacher_end_at) VALUES
    (2, '홍길동', 'teacher01@test.com', 'VOLUNTEER', 2, NULL, '800101', '2026-02-01', NULL);

-- teacher02 / teacher02
INSERT INTO users (id, name, primary_email, role, department_id, classroom_id, resident_registration_number_prefix, teacher_start_at, teacher_end_at) VALUES
    (3, '김철수', 'teacher02@test.com', 'VOLUNTEER', 2, NULL, '850505', '2026-02-01', NULL);

-- guest01 / guest01
INSERT INTO users (id, name, primary_email, role, department_id, classroom_id, resident_registration_number_prefix, teacher_start_at, teacher_end_at) VALUES
    (4, '이영희', 'guest01@test.com', 'GUEST', NULL, NULL, NULL, NULL, NULL);

-- applicant01 / teacher01
INSERT INTO users (id, name, primary_email, role, department_id, classroom_id, resident_registration_number_prefix, teacher_start_at, teacher_end_at) VALUES
    (5, '박지원', 'applicant01@test.com', 'GUEST', NULL, NULL, NULL, NULL, NULL);

-- approved-teacher01 / teacher01
INSERT INTO users (id, name, primary_email, role, department_id, classroom_id, resident_registration_number_prefix, teacher_start_at, teacher_end_at) VALUES
    (6, '최승인', 'approved-teacher01@test.com', 'VOLUNTEER', 2, 2, '910303', '2026-09-01', '2026-12-31');

-- rejected-applicant01 / teacher01
INSERT INTO users (id, name, primary_email, role, department_id, classroom_id, resident_registration_number_prefix, teacher_start_at, teacher_end_at) VALUES
    (7, '정반려', 'rejected-applicant01@test.com', 'GUEST', NULL, NULL, NULL, NULL, NULL);

-- direct-assign-edge01 / teacher01
INSERT INTO users (id, name, primary_email, role, department_id, classroom_id, resident_registration_number_prefix, teacher_start_at, teacher_end_at) VALUES
    (8, '오직접', 'direct-assign-edge01@test.com', 'VOLUNTEER', 2, 2, '930707', NULL, NULL);

-- cancelled-applicant01 / teacher01
INSERT INTO users (id, name, primary_email, role, department_id, classroom_id, resident_registration_number_prefix, teacher_start_at, teacher_end_at) VALUES
    (9, '한취소', 'cancelled-applicant01@test.com', 'GUEST', NULL, NULL, NULL, NULL, NULL);

INSERT INTO users (id, name, primary_email, role, department_id, classroom_id, resident_registration_number_prefix, teacher_start_at, teacher_end_at) VALUES
    (10, 'Apps Script Bot', 'geumjeongyahak-apps-script-bot@gmail.com', 'VOLUNTEER', NULL, NULL, NULL, NULL, NULL);

ALTER SEQUENCE users_id_seq RESTART WITH 11;

-- 3. User Credentials
INSERT INTO user_credentials (id, user_id, provider, credential_email, password_hash, email_verified) VALUES
    (1, 1, 'LOCAL', 'admin@test.com', '$2a$10$A0Av/dPBUz5uoDmp0Z/2S.dsMzOWFL5gLK7CrXmQp6Rw2vqWulapi', TRUE),
    (2, 2, 'LOCAL', 'teacher01@test.com', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE),
    (3, 3, 'LOCAL', 'teacher02@test.com', '$2a$12$jNEpPdWPB8WX6kOR/t9cru3Lz7WwZRw3KHfgoRJBg0ddWUFnymr/O', TRUE),
    (4, 4, 'LOCAL', 'guest01@test.com', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE),
    (5, 5, 'LOCAL', 'applicant01@test.com', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE),
    (6, 6, 'LOCAL', 'approved-teacher01@test.com', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE),
    (7, 7, 'LOCAL', 'rejected-applicant01@test.com', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE),
    (8, 8, 'LOCAL', 'direct-assign-edge01@test.com', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE),
    (9, 9, 'LOCAL', 'cancelled-applicant01@test.com', '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE),
    (10, 10, 'LOCAL', 'geumjeongyahak-apps-script-bot@gmail.com', '$2a$10$6w/cCD2l06.TF3GK7FuCv.uMBX7gqIvgMdwkuSe3D3F6WYbgctibW', TRUE);

ALTER SEQUENCE user_credentials_id_seq RESTART WITH 11;

-- 4. User Permissions
INSERT INTO user_permissions (user_id, permission_code) VALUES
    (2, 'lesson:write:*'),
    (6, 'channel:write:6'),
    (10, 'daily-schedule:manage:*'),
    (10, 'user:read:*'),
    (10, 'purchase-request:read:*'),
    (10, 'purchase-request:manage:*'),
    (10, 'vendor:read:*');

-- 5. Department Permissions
INSERT INTO department_permissions (department_id, role_type, permission_code) VALUES
    (1, 'MEMBER', 'subject:read:*'),
    (1, 'MEMBER', 'lesson:read:*'),
    (1, 'MEMBER', 'channel:write:14'),
    (1, 'MANAGER', 'subject:write:*'),
    (1, 'MANAGER', 'subject:manage:*'),
    (1, 'MANAGER', 'lesson:write:*'),
    (1, 'MANAGER', 'lesson:manage:*'),
    (1, 'MANAGER', 'event:manage:*'),
    (1, 'MANAGER', 'lesson-exchange-request:manage:*'),
    (1, 'MANAGER', 'channel:manage:14'),

    (2, 'MEMBER', 'teacher-application:read:*'),
    (2, 'MEMBER', 'daily-schedule:read:*'),
    (2, 'MEMBER', 'absence-request:read:*'),
    (2, 'MEMBER', 'channel:write:15'),
    (2, 'MANAGER', 'student:write:*'),
    (2, 'MANAGER', 'student:manage:*'),
    (2, 'MANAGER', 'teacher-application:manage:*'),
    (2, 'MANAGER', 'daily-schedule:manage:*'),
    (2, 'MANAGER', 'absence-request:manage:*'),
    (2, 'MANAGER', 'channel:manage:15'),

    (3, 'MEMBER', 'purchase-request:read:*'),
    (3, 'MEMBER', 'channel:write:16'),
    (3, 'MANAGER', 'purchase-request:review:*'),
    (3, 'MANAGER', 'channel:manage:16'),

    (4, 'MEMBER', 'purchase-request:read:*'),
    (4, 'MEMBER', 'vendor:read:*'),
    (4, 'MEMBER', 'channel:write:17'),
    (4, 'MANAGER', 'purchase-request:review:*'),
    (4, 'MANAGER', 'purchase-request:manage:*'),
    (4, 'MANAGER', 'vendor:manage:*'),
    (4, 'MANAGER', 'channel:manage:17'),

    (5, 'MEMBER', 'teacher-application:read:*'),
    (5, 'MEMBER', 'channel:write:18'),
    (5, 'MANAGER', 'channel:manage:18');

-- 6-1. Site Contents
INSERT INTO site_contents (id, content_type, ref_id, title, name, content_group, sort_order)
VALUES
    (1, 'PRINCIPAL', NULL, '교장', '정해웅', NULL, 1),
    (2, 'DEPARTMENT', 1, '교무기획부', NULL, NULL, 2),
    (3, 'DEPARTMENT', 2, '교육연구부', NULL, NULL, 3),
    (4, 'DEPARTMENT', 3, '생활안전부', NULL, NULL, 4),
    (5, 'DEPARTMENT', 4, '총무부', NULL, NULL, 5),
    (6, 'DEPARTMENT', 5, '홍보부', NULL, NULL, 6),
    (7, 'DEPARTMENT', 6, '편집부', NULL, NULL, 7),
    (8, 'CLASSROOM', 1, '벚꽃반', NULL, 'WEEKDAY', 1),
    (9, 'CLASSROOM', 2, '개나리반', NULL, 'WEEKDAY', 2),
    (10, 'CLASSROOM', 3, '민들레반', NULL, 'WEEKDAY', 3),
    (11, 'CLASSROOM', 4, '동백반', NULL, 'WEEKDAY', 4),
    (12, 'CLASSROOM', 5, '해바라기반', NULL, 'WEEKDAY', 5),
    (13, 'CLASSROOM', 6, '국화반', NULL, 'WEEKDAY', 6),
    (14, 'CLASSROOM', 7, '주말 영어반', NULL, 'WEEKEND_MORNING', 1),
    (15, 'CLASSROOM', 8, '주말 스마트폰반', NULL, 'WEEKEND_AFTERNOON', 1),
    (16, 'CLASSROOM', 9, '겨울반', NULL, 'WEEKDAY', 7);
ALTER SEQUENCE site_contents_id_seq RESTART WITH 17;

INSERT INTO site_content_items (id, site_content_id, content, sort_order)
VALUES
    (1, 1, '금정열린배움터의 전반적인 운영을 총괄', 1),
    (2, 1, '학생, 교사, 운영진이 함께 배우는 야학 문화를 지키는 역할', 2),
    (3, 2, '학기 시간표 편성 및 분반별 수업 운영 점검', 1),
    (4, 2, '교사 배정, 보강 일정, 학사 공지 조율', 2),
    (5, 3, '신입 교원 면접과 참관 수업 운영', 1),
    (6, 3, '수업 일지, 생일, 연구 수업, 교육 자료 관리', 2),
    (7, 3, '교사 교육과 수업 품질 개선 논의 진행', 3),
    (8, 4, '학생 생활 상담, 출결 확인, 안전 귀가 안내', 1),
    (9, 4, '분반별 생활 이슈 공유 및 지원 연계', 2),
    (10, 5, '기관 행정, 회계 보조, 비품과 시설 사용 관리', 1),
    (11, 5, '구매 요청, 회의 준비, 문서 보관 지원', 2),
    (12, 6, '기관 소식, 행사 안내, 대외 홍보 콘텐츠 운영', 1),
    (13, 6, '사진 기록과 카드뉴스, SNS 게시물 제작', 2),
    (14, 7, '소식지, 회의록, 각종 안내문 편집 및 발행', 1),
    (15, 7, '학생 글, 수업 후기, 행사 기록 정리', 2),
    (16, 8, '평일 저녁 문해 기초와 생활 한글을 차근차근 배우는 반', 1),
    (17, 8, '수요일 19:20~21:40 중심으로 운영되는 기초 학습반', 2),
    (18, 9, '생활 수학과 문해 복습을 병행하는 평일 초급 반', 1),
    (19, 9, '수요일 저녁과 금요일 보강 시간표를 함께 확인하는 반', 2),
    (20, 10, '민들레반은 문해와 스마트폰 기초 활용을 연결해 배우는 반', 1),
    (21, 10, '주말 수업과 연계해 반복 학습을 진행', 2),
    (22, 11, '동백반은 평일 보충 수업과 개별 학습 피드백 중심으로 운영', 1),
    (23, 12, '해바라기반은 활동형 읽기, 말하기, 쓰기 수업을 운영', 1),
    (24, 13, '국화반은 신규 교원 배정 후보가 있는 목요일 저녁 시간표 반', 1),
    (25, 13, '교원 신청과 관리자 배정 화면 테스트에 활용 가능', 2),
    (26, 14, '주말 오전 영어 기초, 생활 표현, 읽기 연습 중심 반', 1),
    (27, 15, '주말 오후 스마트폰 활용, 사진 보내기, 앱 사용법을 배우는 반', 1),
    (28, 16, '겨울반은 방학 기간 보충 학습과 계절 특강을 운영하는 반', 1);
ALTER SEQUENCE site_content_items_id_seq RESTART WITH 29;

INSERT INTO site_histories (id, title, detail, history_date, sort_order)
VALUES
    (1, '1997년', '금정열린배움터 설립', DATE '1997-01-01', 1),
    (2, '2002년', '문해 기초반과 생활 한글 수업을 정규 과정으로 정비', DATE '2002-03-01', 2),
    (3, '2008년', '교사 회의와 분반별 수업 기록 체계를 도입', DATE '2008-03-01', 3),
    (4, '2014년', '주말반을 확대하고 스마트폰 활용 수업을 시범 운영', DATE '2014-09-01', 4),
    (5, '2019년', '학생 출석과 수업 일지를 디지털 방식으로 병행 관리', DATE '2019-03-01', 5),
    (6, '2023년', '교학회의록, 행사 기록, 요청 업무를 통합 관리하는 운영 체계 정비', DATE '2023-03-01', 6),
    (7, '2026년', '교원 신청과 시간표 배정 흐름을 온라인으로 전환', DATE '2026-06-01', 7);
ALTER SEQUENCE site_histories_id_seq RESTART WITH 8;

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
    (19, '편집부', '편집부 전용 채널', 'DEPARTMENT', 'DOMAIN_LINKED', 6, 'READ_WRITE', FALSE, FALSE, TRUE),
    (20, '인수인계서', '교원 인수인계서 자료를 공유하는 채널', 'RESOURCE', 'STANDALONE', NULL, 'READ_ONLY', FALSE, FALSE, TRUE),
    (21, '시험 문제 자료', '시험 문제와 평가 자료를 공유하는 채널', 'RESOURCE', 'STANDALONE', NULL, 'READ_ONLY', FALSE, FALSE, TRUE),
    (22, '서류 양식', '운영에 필요한 각종 서류 양식을 공유하는 채널', 'RESOURCE', 'STANDALONE', NULL, 'READ_ONLY', FALSE, FALSE, TRUE),
    (23, '교칙', '교칙 안내문을 게시하는 기본 안내 채널', 'GUIDE', 'STANDALONE', NULL, 'READ_ONLY', TRUE, TRUE, TRUE),
    (24, '교사 신청 안내', '교사 신청 안내문을 게시하는 기본 안내 채널', 'GUIDE', 'STANDALONE', NULL, 'READ_ONLY', TRUE, TRUE, TRUE);
ALTER SEQUENCE channels_id_seq RESTART WITH 25;

-- 7-1. Posts
INSERT INTO posts (
    id, channel_id, author_id, title, content_html, status, is_pinned, allow_comment,
    thumbnail_url, expires_at, view_count, is_deleted, created_at, updated_at
)
VALUES
    (1, 2, 1, '6월 수업 운영 안내',
     '<p>6월 정규 수업은 기존 시간표대로 운영합니다. 분반별 보강 일정은 담당 교사와 교무기획부 공지를 확인해 주세요.</p>',
     'PUBLISHED', TRUE, FALSE, NULL, '2026-06-30 23:59:59', 152, FALSE, '2026-06-01 09:00:00', '2026-06-01 09:00:00'),
    (2, 2, 1, '신입 교원 오리엔테이션 참석 안내',
     '<p>신입 교원 오리엔테이션은 6월 12일 금요일 19시에 진행됩니다. 수업 운영 방식과 출석 기록 방법을 함께 안내합니다.</p>',
     'PUBLISHED', TRUE, FALSE, NULL, '2026-06-12 21:00:00', 128, FALSE, '2026-05-31 18:00:00', '2026-05-31 18:00:00'),
    (3, 2, 1, '교실 정리 및 비품 사용 안내',
     '<p>수업 후 칠판 정리, 책상 배열, 공용 교재 반납을 부탁드립니다. 소모품 부족 시 총무부 구매 요청으로 남겨 주세요.</p>',
     'PUBLISHED', FALSE, FALSE, NULL, NULL, 89, FALSE, '2026-05-30 17:20:00', '2026-05-30 17:20:00'),
    (4, 2, 1, '6월 첫째 주 출석 기록 확인 요청',
     '<p>담당 교사는 수업 종료 후 당일 출석과 수업 일지를 확인해 주세요. 누락된 기록은 교무기획부에서 별도 확인합니다.</p>',
     'PUBLISHED', FALSE, FALSE, NULL, NULL, 74, FALSE, '2026-05-29 13:00:00', '2026-05-29 13:00:00'),
    (5, 2, 1, '주말 스마트폰반 보조 교사 모집',
     '<p>토요일 스마트폰 활용 수업에 함께할 보조 교사를 모집합니다. 가능한 날짜를 교무기획부에 알려 주세요.</p>',
     'PUBLISHED', FALSE, FALSE, NULL, '2026-06-15 23:59:59', 96, FALSE, '2026-05-28 10:30:00', '2026-05-28 10:30:00'),
    (6, 2, 1, '상반기 수업 회고 자료 제출 안내',
     '<p>상반기 수업 회고에 사용할 분반별 의견을 6월 말까지 제출해 주세요. 수업 자료와 학생 반응을 함께 정리합니다.</p>',
     'PUBLISHED', FALSE, FALSE, NULL, NULL, 63, FALSE, '2026-05-27 19:10:00', '2026-05-27 19:10:00'),
    (7, 2, 1, '개인정보 보호와 학생 사진 촬영 안내',
     '<p>학생 사진 촬영과 외부 공유는 사전 동의가 있는 경우에만 가능합니다. 행사 기록도 내부 공유 범위를 먼저 확인해 주세요.</p>',
     'PUBLISHED', FALSE, FALSE, NULL, NULL, 141, FALSE, '2026-05-26 11:00:00', '2026-05-26 11:00:00'),
    (8, 2, 1, '회의실 사용 예약 방법 안내',
     '<p>교학회의, 면담, 수업 준비 모임은 사전에 회의실 사용 시간을 공유해 주세요. 중복 시 운영진이 조정합니다.</p>',
     'PUBLISHED', FALSE, FALSE, NULL, NULL, 58, FALSE, '2026-05-25 16:40:00', '2026-05-25 16:40:00'),
    (9, 2, 1, '학습 자료 업로드 위치 안내',
     '<p>공용 학습 자료는 자료실 채널에 업로드해 주세요. 분반 전용 자료는 각 분반 채널을 사용합니다.</p>',
     'PUBLISHED', FALSE, FALSE, NULL, NULL, 77, FALSE, '2026-05-24 09:50:00', '2026-05-24 09:50:00'),
    (10, 2, 1, '6월 생일 축하 명단 확인',
     '<p>6월 생일 축하 준비를 위해 명단을 확인합니다. 누락된 학생이나 교사가 있으면 교육연구부에 알려 주세요.</p>',
     'PUBLISHED', FALSE, FALSE, NULL, NULL, 51, FALSE, '2026-05-23 15:10:00', '2026-05-23 15:10:00'),
    (11, 2, 1, '우천 시 등하교 안전 안내',
     '<p>비가 많이 오는 날에는 학생 귀가 동선을 함께 확인해 주세요. 우산 비치 위치는 총무부 공지를 참고합니다.</p>',
     'PUBLISHED', FALSE, FALSE, NULL, NULL, 69, FALSE, '2026-05-22 08:30:00', '2026-05-22 08:30:00'),
    (12, 2, 1, '신규 학생 상담 일정 공유',
     '<p>신규 학생 상담은 생활안전부와 교무기획부가 함께 진행합니다. 분반 배정 전 기초 학습 상황을 확인합니다.</p>',
     'PUBLISHED', FALSE, FALSE, NULL, NULL, 83, FALSE, '2026-05-21 12:00:00', '2026-05-21 12:00:00'),
    (13, 4, 1, '신입 교원 오리엔테이션 현장',
     '<p>신입 교원과 운영진이 모여 수업 운영 흐름과 학생 응대 원칙을 함께 확인했습니다.</p>',
     'PUBLISHED', FALSE, FALSE, '/mock/event-orientation.png', NULL, 112, FALSE, '2026-06-02 20:30:00', '2026-06-02 20:30:00'),
    (14, 4, 1, '상반기 수업 회고 모임',
     '<p>분반별 수업 경험을 나누고 하반기 개선 방향을 정리한 회고 모임입니다.</p>',
     'PUBLISHED', FALSE, FALSE, '/mock/event-review.png', NULL, 94, FALSE, '2026-05-29 21:00:00', '2026-05-29 21:00:00'),
    (15, 4, 1, '주말 스마트폰반 실습 수업',
     '<p>사진 보내기, 길 찾기, 메시지 확인을 직접 연습한 주말 스마트폰반 수업 기록입니다.</p>',
     'PUBLISHED', FALSE, FALSE, '/mock/event-smartphone.png', NULL, 87, FALSE, '2026-05-24 12:20:00', '2026-05-24 12:20:00'),
    (16, 4, 1, '학생 발표회 준비 스케치',
     '<p>학생 발표회를 준비하며 낭독과 생활 글쓰기 발표 순서를 맞춰 본 날의 기록입니다.</p>',
     'PUBLISHED', FALSE, FALSE, '/mock/event-presentation.png', NULL, 66, FALSE, '2026-05-18 18:40:00', '2026-05-18 18:40:00'),
    (17, 4, 1, '생활 문해 낭독 모임',
     '<p>학생들이 직접 고른 생활 글을 함께 읽고 짧은 소감을 나눈 낭독 모임입니다.</p>',
     'PUBLISHED', FALSE, FALSE, '/mock/event-reading.png', NULL, 72, FALSE, '2026-05-14 19:30:00', '2026-05-14 19:30:00'),
    (18, 4, 1, '교사 수업 준비 워크숍',
     '<p>신규 교안 작성법과 수업 자료 정리 방식을 함께 실습한 교사 워크숍입니다.</p>',
     'PUBLISHED', FALSE, FALSE, '/mock/event-workshop.png', NULL, 58, FALSE, '2026-05-10 14:00:00', '2026-05-10 14:00:00'),
    (19, 4, 1, '야외 체험 활동 기록',
     '<p>분반 학생들과 가까운 생활 공간을 걸으며 읽기와 표현 활동을 연결한 체험 기록입니다.</p>',
     'PUBLISHED', FALSE, FALSE, '/mock/event-field-day.png', NULL, 61, FALSE, '2026-05-04 16:20:00', '2026-05-04 16:20:00'),
    (20, 4, 1, '작은 발표 전시회',
     '<p>한 학기 동안 쓴 글과 그림을 모아 서로에게 소개한 작은 발표 전시회입니다.</p>',
     'PUBLISHED', FALSE, FALSE, '/mock/event-exhibition.png', NULL, 49, FALSE, '2026-04-29 18:00:00', '2026-04-29 18:00:00');
ALTER SEQUENCE posts_id_seq RESTART WITH 21;

-- 8. Subjects
INSERT INTO subjects (id, class_id, teacher_id, name, start_at, end_at, day_of_week, start_time, end_time, period, teacher_assigned_at, description)
VALUES
    (1, 1, 2, '한글 기초', '2026-02-01', '2026-06-30', 'WEDNESDAY',  '19:20:00', '20:00:00', 1, '2026-02-01 00:00:00', '기초 한글 수업'),
    (2, 2, 3, '수학 기초', '2026-02-01', '2026-06-30', 'WEDNESDAY',  '19:20:00', '20:00:00', 1, '2026-02-01 00:00:00', '기초 수학 수업'),
    (3, 8, 2, '스마트폰 활용', '2026-02-01', '2026-06-30', 'SATURDAY', '10:00:00', '11:20:00', 1, '2026-02-01 00:00:00', '스마트폰 사용법'),
    (4, 1, 2, '한글 읽기', '2026-09-01', '2026-12-31', 'MONDAY', '19:20:00', '20:00:00', 1, '2026-06-01 00:00:00', '벚꽃반 월요일 1교시 배정 예시'),
    (5, 1, 2, '생활 문해', '2026-09-01', '2026-12-31', 'MONDAY', '20:10:00', '20:50:00', 2, '2026-06-01 00:00:00', '벚꽃반 월요일 2교시 배정 예시'),
    (6, 6, NULL, '국어 기초', '2026-09-01', '2026-12-31', 'THURSDAY', '19:20:00', '20:00:00', 1, NULL, '교원 신청 가능 미배정 시간표 1교시'),
    (7, 6, NULL, '수학 기초', '2026-09-01', '2026-12-31', 'THURSDAY', '20:10:00', '20:50:00', 2, NULL, '교원 신청 가능 미배정 시간표 2교시'),
    (8, 8, NULL, '스마트폰 첫걸음', '2026-09-05', '2026-12-26', 'SATURDAY', '10:00:00', '11:20:00', 1, NULL, '주말 교원 신청 가능 미배정 시간표'),
    (9, 2, 6, '문해 복습', '2026-09-01', '2026-12-31', 'FRIDAY', '19:20:00', '20:00:00', 1, '2026-06-01 00:00:00', '승인된 교원 신청으로 배정된 시간표 1교시'),
    (10, 2, 6, '생활 수학', '2026-09-01', '2026-12-31', 'FRIDAY', '20:10:00', '20:50:00', 2, '2026-06-01 00:00:00', '승인된 교원 신청으로 배정된 시간표 2교시'),
    (11, 1, 2, '한글 읽기', '2026-02-01', '2026-06-30', 'WEDNESDAY', '20:10:00', '20:50:00', 2, '2026-02-01 00:00:00', '벚꽃반 2교시 읽기 수업'),
    (12, 1, 2, '생활 문해', '2026-02-01', '2026-06-30', 'WEDNESDAY', '21:00:00', '21:40:00', 3, '2026-02-01 00:00:00', '벚꽃반 3교시 생활 문해 수업'),
    (13, 2, 3, '생활 수학', '2026-02-01', '2026-06-30', 'WEDNESDAY', '20:10:00', '20:50:00', 2, '2026-02-01 00:00:00', '개나리반 2교시 생활 수학 수업'),
    (14, 2, 3, '수학 응용', '2026-02-01', '2026-06-30', 'WEDNESDAY', '21:00:00', '21:40:00', 3, '2026-02-01 00:00:00', '개나리반 3교시 수학 응용 수업');
ALTER SEQUENCE subjects_id_seq RESTART WITH 15;

-- 8-1. Teacher Applications and Schedule Assignments
INSERT INTO teacher_applications (
    id, applicant_id, applicant_name, applicant_phone_number, applicant_email,
    birth_date, address, education_and_major, preferred_subject_id,
    motivation, desired_teacher_image, meaning_of_sharing,
    status, reviewed_at, reviewed_by, review_note, created_at, updated_at
)
VALUES
    (1, 5, '박지원', '010-5555-0101', 'applicant01@test.com',
     '1998-04-12', '부산광역시 금정구', '부산대학교 국어교육과 재학', 6,
     '평일 저녁 문해 수업에 꾸준히 참여하고 싶습니다.',
     '학생의 속도에 맞춰 기다리는 선생님이 되고 싶습니다.',
     '제가 가진 시간을 지역의 배움과 나누는 일이라고 생각합니다.',
     'PENDING', NULL, NULL, NULL, '2026-06-01 10:00:00', '2026-06-01 10:00:00'),
    (2, 6, '최승인', '010-5555-0202', 'approved-teacher01@test.com',
     '1995-09-20', '부산광역시 동래구', '동아대학교 수학교육과 졸업', 9,
     '금요일 저녁 시간표를 맡아 장기적으로 수업하고 싶습니다.',
     '수업 준비와 기록을 성실히 남기는 선생님이 되고 싶습니다.',
     '배움의 기회를 함께 만드는 일이라고 생각합니다.',
     'APPROVED', '2026-06-01 14:00:00', 1, '면접 후 금요일 개나리반 시간표로 승인', '2026-05-29 09:30:00', '2026-06-01 14:00:00'),
    (3, 7, '정반려', '010-5555-0303', 'rejected-applicant01@test.com',
     '1997-12-03', '부산광역시 해운대구', '부경대학교 영어영문학과 졸업', 8,
     '주말 스마트폰반 봉사에 관심이 있어 지원했습니다.',
     '쉽게 설명하고 반복해서 도와주는 선생님이 되고 싶습니다.',
     '기술 사용에 어려움을 겪는 분들과 경험을 나누는 일이라고 생각합니다.',
     'REJECTED', '2026-06-02 11:00:00', 1, '활동 가능 기간 재확인 필요', '2026-05-30 16:20:00', '2026-06-02 11:00:00'),
    (4, 9, '한취소', '010-5555-0404', 'cancelled-applicant01@test.com',
     '1999-08-14', '부산광역시 금정구', '부산대학교 사회학과 재학', 8,
     '주말 스마트폰반 활동을 희망했지만 일정이 변경되었습니다.',
     '차분히 반복 설명하는 선생님이 되고 싶습니다.',
     '필요한 곳에 시간을 나누는 일이라고 생각합니다.',
     'CANCELLED', NULL, NULL, NULL, '2026-06-01 12:00:00', '2026-06-01 12:30:00');
ALTER SEQUENCE teacher_applications_id_seq RESTART WITH 5;

INSERT INTO teacher_schedule_assignments (id, teacher_application_id, subject_id, created_at, updated_at)
VALUES
    (1, 2, 9, '2026-06-01 14:00:00', '2026-06-01 14:00:00'),
    (2, 2, 10, '2026-06-01 14:00:00', '2026-06-01 14:00:00');
ALTER SEQUENCE teacher_schedule_assignments_id_seq RESTART WITH 3;

-- 9. Lessons
INSERT INTO lessons (id, subject_id, teacher_id, period, date, start_time, end_time, status)
VALUES
    (1, 1, 2, 1, '2026-06-10', '19:20:00', '20:00:00', 'SCHEDULED'),
    (2, 11, 2, 2, '2026-06-10', '20:10:00', '20:50:00', 'SCHEDULED'),
    (3, 12, 2, 3, '2026-06-10', '21:00:00', '21:40:00', 'SCHEDULED'),
    (4, 2, 3, 1, '2026-06-17', '19:20:00', '20:00:00', 'SCHEDULED'),
    (5, 13, 3, 2, '2026-06-17', '20:10:00', '20:50:00', 'SCHEDULED'),
    (6, 14, 3, 3, '2026-06-17', '21:00:00', '21:40:00', 'SCHEDULED'),
    (7, 1, 2, 1, '2026-05-13', '19:20:00', '20:00:00', 'COMPLETED'),
    (8, 11, 2, 2, '2026-05-13', '20:10:00', '20:50:00', 'COMPLETED'),
    (9, 12, 2, 3, '2026-05-13', '21:00:00', '21:40:00', 'COMPLETED'),
    (10, 4, 2, 1, '2026-09-07', '19:20:00', '20:00:00', 'SCHEDULED'),
    (11, 5, 2, 2, '2026-09-07', '20:10:00', '20:50:00', 'SCHEDULED'),
    (12, 9, 6, 1, '2026-09-04', '19:20:00', '20:00:00', 'SCHEDULED'),
    (13, 10, 6, 2, '2026-09-04', '20:10:00', '20:50:00', 'SCHEDULED'),
    (14, 4, 2, 1, '2026-09-14', '19:20:00', '20:00:00', 'SCHEDULED'),
    (15, 5, 2, 2, '2026-09-14', '20:10:00', '20:50:00', 'SCHEDULED'),
    (16, 9, 6, 1, '2026-09-11', '19:20:00', '20:00:00', 'SCHEDULED'),
    (17, 10, 6, 2, '2026-09-11', '20:10:00', '20:50:00', 'SCHEDULED'),
    (18, 1, 2, 1, '2026-06-03', '19:20:00', '20:00:00', 'SCHEDULED'),
    (19, 11, 2, 2, '2026-06-03', '20:10:00', '20:50:00', 'SCHEDULED'),
    (20, 12, 2, 3, '2026-06-03', '21:00:00', '21:40:00', 'SCHEDULED'),
    (21, 2, 3, 1, '2026-06-03', '19:20:00', '20:00:00', 'SCHEDULED'),
    (22, 13, 3, 2, '2026-06-03', '20:10:00', '20:50:00', 'SCHEDULED'),
    (23, 14, 3, 3, '2026-06-03', '21:00:00', '21:40:00', 'SCHEDULED'),
    (24, 3, 2, 1, '2026-06-06', '10:00:00', '11:20:00', 'SCHEDULED'),
    (25, 1, 2, 1, '2026-06-24', '19:20:00', '20:00:00', 'SCHEDULED'),
    (26, 11, 2, 2, '2026-06-24', '20:10:00', '20:50:00', 'SCHEDULED'),
    (27, 12, 2, 3, '2026-06-24', '21:00:00', '21:40:00', 'SCHEDULED'),
    (28, 2, 3, 1, '2026-06-24', '19:20:00', '20:00:00', 'SCHEDULED'),
    (29, 13, 3, 2, '2026-06-24', '20:10:00', '20:50:00', 'SCHEDULED'),
    (30, 14, 3, 3, '2026-06-24', '21:00:00', '21:40:00', 'SCHEDULED'),
    (31, 3, 2, 1, '2026-06-27', '10:00:00', '11:20:00', 'SCHEDULED');
UPDATE lessons SET note = '1교시에는 한글 자음과 모음 복습을 진행했습니다.' WHERE id = 7;
UPDATE lessons SET note = '2교시에는 짧은 단어 읽기와 받아쓰기를 연습했습니다.' WHERE id = 8;
UPDATE lessons SET note = '3교시에는 생활 문장 읽기 활동과 개별 피드백을 진행했습니다.' WHERE id = 9;
ALTER SEQUENCE lessons_id_seq RESTART WITH 32;

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
    (5, 8, 2, '2026-06-27', '10:00:00', '11:20:00', 'SCHEDULED', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (6, 1, 2, '2026-05-13', '19:20:00', '21:40:00', 'COMPLETED', FALSE, '2026-05-13 18:00:00', '2026-05-13 22:00:00'),
    (7, 1, 2, '2026-09-07', '19:20:00', '20:50:00', 'SCHEDULED', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (8, 2, 6, '2026-09-04', '19:20:00', '20:50:00', 'SCHEDULED', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (9, 1, 2, '2026-09-14', '19:20:00', '20:50:00', 'SCHEDULED', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (10, 2, 6, '2026-09-11', '19:20:00', '20:50:00', 'SCHEDULED', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (11, 1, 2, '2026-06-03', '19:20:00', '21:40:00', 'SCHEDULED', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (12, 2, 3, '2026-06-03', '19:20:00', '21:40:00', 'SCHEDULED', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (13, 8, 2, '2026-06-06', '10:00:00', '11:20:00', 'SCHEDULED', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00');
UPDATE daily_schedules
SET resident_registration_number_prefix = '900101',
    personal_info_consent = TRUE
WHERE id = 6;
ALTER SEQUENCE daily_schedules_id_seq RESTART WITH 14;

-- 10-1. Daily Teacher Attendances
INSERT INTO daily_teacher_attendances (
    id, daily_schedule_id, status, volunteer_service_minutes, attended_at, latitude, longitude, is_deleted,
    created_at, updated_at
)
VALUES
    (1, 6, 'PRESENT', 140, '2026-05-13 19:18:00', NULL, NULL, FALSE, '2026-05-13 19:18:00', '2026-05-13 19:18:00'),
    (2, 7, 'ABSENT', 90, NULL, NULL, NULL, FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (3, 8, 'ABSENT', 90, NULL, NULL, NULL, FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (4, 9, 'ABSENT', 90, NULL, NULL, NULL, FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (5, 10, 'ABSENT', 90, NULL, NULL, NULL, FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (6, 1, 'ABSENT', 140, NULL, NULL, NULL, FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (7, 2, 'ABSENT', 140, NULL, NULL, NULL, FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (8, 3, 'ABSENT', 140, NULL, NULL, NULL, FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (9, 4, 'EXCUSED', 140, NULL, NULL, NULL, FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (10, 5, 'ABSENT', 80, NULL, NULL, NULL, FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (11, 11, 'ABSENT', 140, NULL, NULL, NULL, FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (12, 12, 'ABSENT', 140, NULL, NULL, NULL, FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (13, 13, 'ABSENT', 80, NULL, NULL, NULL, FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00');
ALTER SEQUENCE daily_teacher_attendances_id_seq RESTART WITH 14;

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
    (1, 3, '2026-06-24', 2, '6월 24일 한글 기초 수업 교환 요청', '벚꽃반', '개인 일정으로 6월 24일 한글 기초 수업 교환을 요청합니다.', 'PENDING',
     '2026-06-21 23:59:00', NULL, NULL, NULL, NULL, NULL, '2026-05-20 10:00:00', '2026-05-20 10:00:00'),
    (2, 2, '2026-06-17', 3, '6월 17일 수학 기초 수업 교환 요청', '개나리반', '해당 날짜 수학 기초 수업을 다른 선생님과 교환하고 싶습니다.', 'PENDING',
     '2026-06-14 23:59:00', NULL, NULL, NULL, NULL, NULL, '2026-05-21 09:30:00', '2026-05-21 09:30:00'),
    (3, 1, '2026-06-10', 2, '승인된 한글 기초 수업 교환 요청', '벚꽃반', '관리자가 승인해 제안을 받을 수 있는 수업 교환 요청입니다.', 'APPROVED',
     '2026-06-07 23:59:00', '2026-05-22 11:00:00', 1, NULL, NULL, NULL, '2026-05-21 13:10:00', '2026-05-22 11:00:00'),
    (4, 4, '2026-06-24', 3, '반려된 수학 기초 수업 교환 요청', '개나리반', '일정상 교환 요청을 제출했으나 반려된 예시입니다.', 'REJECTED',
     '2026-06-21 23:59:00', '2026-05-23 14:20:00', 1, NULL, NULL, '대체 수업 일정이 충분하지 않습니다.', '2026-05-22 15:00:00', '2026-05-23 14:20:00'),
    (5, 11, '2026-06-03', 2, '완료된 한글 기초 수업 교환 요청', '벚꽃반', '제안 수락까지 완료된 수업 교환 요청입니다.', 'COMPLETED',
     '2026-05-31 23:59:00', '2026-05-24 10:00:00', 1, '2026-05-25 16:00:00', NULL, NULL, '2026-05-23 08:40:00', '2026-05-25 16:00:00'),
    (6, 12, '2026-06-03', 3, '만료된 수학 기초 수업 교환 요청', '개나리반', '만료 처리된 수업 교환 요청입니다.', 'EXPIRED',
     '2026-05-24 23:59:00', NULL, NULL, NULL, NULL, NULL, '2026-05-20 16:20:00', '2026-05-25 00:10:00'),
    (7, 13, '2026-06-06', 2, '취소된 스마트폰 활용 수업 교환 요청', '주말 스마트폰반', '요청자가 직접 취소한 수업 교환 요청입니다.', 'CANCELLED',
     '2026-06-03 23:59:00', NULL, NULL, NULL, '2026-05-24 18:00:00', NULL, '2026-05-24 12:00:00', '2026-05-24 18:00:00');
ALTER SEQUENCE lesson_exchange_requests_id_seq RESTART WITH 8;

-- 13. Lesson Exchange Proposals
INSERT INTO lesson_exchange_proposals (
    id, request_id, proposed_by, proposal_type, daily_schedule_id, lesson_date, content, classroom_name_snapshot,
    status, accepted_at, withdrawn_at, closed_at, created_at, updated_at
)
VALUES
    (1, 3, 3, 'EXCHANGE', 2, '2026-06-17', '처음 제안한 교환 일정을 철회했습니다.', '개나리반',
     'WITHDRAWN', NULL, '2026-05-23 08:00:00', NULL, '2026-05-22 13:00:00', '2026-05-23 08:00:00'),
    (2, 3, 1, 'SUBSTITUTION', NULL, NULL, '교환 대신 6월 10일 한글 기초 수업을 대체할 수 있습니다.', NULL,
     'ACTIVE', NULL, NULL, NULL, '2026-05-23 08:30:00', '2026-05-23 08:30:00'),
    (3, 3, 3, 'EXCHANGE', 2, '2026-06-17', '철회 후 일정을 확인해 다시 제출한 교환 제안입니다.', '개나리반',
     'ACTIVE', NULL, NULL, NULL, '2026-05-23 12:40:00', '2026-05-23 12:40:00'),
    (4, 5, 3, 'EXCHANGE', 2, '2026-06-17', '완료된 요청에서 수락된 교환 제안입니다.', '개나리반',
     'ACCEPTED', '2026-05-25 16:00:00', NULL, NULL, '2026-05-24 09:00:00', '2026-05-25 16:00:00'),
    (5, 5, 1, 'SUBSTITUTION', NULL, NULL, '다른 제안이 수락되어 종료된 대체 제안입니다.', NULL,
     'CLOSED', NULL, NULL, '2026-05-25 16:00:00', '2026-05-24 10:30:00', '2026-05-25 16:00:00');
ALTER SEQUENCE lesson_exchange_proposals_id_seq RESTART WITH 6;

-- 13-1. Meeting Records
INSERT INTO meeting_records (
    id, author_id, title, agenda, discussion, suggestion, status, is_deleted, view_count, created_at, updated_at
)
VALUES
    (1, 1, '2026년 6월 첫째 주 교학회의',
     '6월 수업 운영, 신입 교원 오리엔테이션, 출석 기록 점검',
     '분반별 수업은 기존 시간표대로 운영하고, 신규 교원에게 수업 일지 작성 방법을 안내하기로 했습니다. 국화반 목요일 시간표는 교원 신청 승인 후 배정 현황을 다시 확인합니다.',
     '프론트 메인 화면에서 공지와 이번 주 수업을 함께 볼 수 있도록 운영 데이터를 주기적으로 보강합니다.',
     'AFTER_MEETING', FALSE, 34, '2026-06-02 21:20:00', '2026-06-02 21:20:00'),
    (2, 1, '2026년 5월 넷째 주 교학회의',
     '상반기 수업 회고, 행사 사진 기록, 학생 발표회 준비',
     '상반기 회고 모임 사진은 행사안내 채널에 게시하고, 학생 발표회는 분반별로 발표 가능한 주제를 먼저 모으기로 했습니다.',
     '행사 사진은 썸네일이 없는 경우 목록 가독성이 떨어지므로 mock 썸네일이라도 반드시 연결합니다.',
     'AFTER_MEETING', FALSE, 28, '2026-05-27 21:10:00', '2026-05-27 21:10:00'),
    (3, 1, '2026년 5월 셋째 주 교학회의',
     '주말 스마트폰반 운영, 보조 교사 모집, 학생 안전 귀가',
     '스마트폰반은 실습 중심으로 운영하고, 보조 교사 모집 공지를 공지사항에 올리기로 했습니다. 우천 시 귀가 동선은 생활안전부가 확인합니다.',
     '주말 수업은 평일 수업과 충돌하지 않도록 교사 배정 화면에서 요일과 날짜를 함께 표시합니다.',
     'AFTER_MEETING', FALSE, 22, '2026-05-20 21:00:00', '2026-05-20 21:00:00'),
    (4, 1, '2026년 5월 둘째 주 교학회의',
     '교실 비품, 자료실 정리, 구매 요청 처리',
     '공용 교재와 필기구 재고를 확인하고 부족한 품목은 구매 요청으로 올리기로 했습니다. 자료실 채널의 파일 분류도 함께 정리합니다.',
     '구매 요청 상세 화면에서 거래 품목명이 누락되지 않도록 회귀 테스트를 유지합니다.',
     'AFTER_MEETING', FALSE, 19, '2026-05-13 21:30:00', '2026-05-13 21:30:00'),
    (5, 1, '2026년 5월 첫째 주 교학회의',
     '신규 학생 상담, 분반 배정, 수업 참관 일정',
     '신규 학생 상담 후 벚꽃반과 개나리반에 우선 배정하고, 참관 가능한 교사는 교육연구부에 가능한 날짜를 공유하기로 했습니다.',
     '학생 정보 페이지에서 분반별 학생 수를 확인할 수 있도록 seed 데이터를 유지합니다.',
     'AFTER_MEETING', FALSE, 25, '2026-05-06 21:00:00', '2026-05-06 21:00:00'),
    (6, 1, '2026년 6월 둘째 주 교학회의 준비',
     '오리엔테이션 이후 피드백, 국화반 교원 배정, 6월 생일 축하 준비',
     NULL,
     NULL,
     'BEFORE_MEETING', FALSE, 7, '2026-06-03 10:00:00', '2026-06-03 10:00:00');
ALTER SEQUENCE meeting_records_id_seq RESTART WITH 7;

-- 13-2. Events
INSERT INTO events (
    id, title, description, event_date, start_time, end_time, created_by_id, updated_by_id, is_deleted,
    created_at, updated_at
)
VALUES
    (1, '신입 교원 오리엔테이션', '신규 교원 신청자와 승인 교원을 대상으로 수업 운영 방식을 안내합니다.',
     '2026-06-12', '19:00:00', '20:30:00', 1, 1, FALSE, '2026-06-01 09:00:00', '2026-06-01 09:00:00'),
    (2, '9월 개강 준비 회의', '2학기 시간표와 분반별 담당 교원을 최종 확인합니다.',
     '2026-08-28', '19:30:00', '21:00:00', 1, 1, FALSE, '2026-06-01 09:10:00', '2026-06-01 09:10:00'),
    (3, '추석 연휴 휴강 안내', '추석 연휴 기간 수업 운영 여부를 안내합니다.',
     '2026-09-25', NULL, NULL, 1, 1, FALSE, '2026-06-01 09:20:00', '2026-06-01 09:20:00'),
    (4, '상반기 수업 회고', '상반기 수업 운영 내용을 돌아보고 개선점을 정리합니다.',
     '2026-05-29', '19:00:00', '20:00:00', 1, 1, FALSE, '2026-05-20 09:00:00', '2026-05-20 09:00:00');
ALTER SEQUENCE events_id_seq RESTART WITH 5;

-- 14. Vendors
INSERT INTO vendors (id, name, description, balance, is_active)
VALUES
    (1, '예소디자인', '부산대 인근 거래처', 0, TRUE),
    (2, '목민서관', '야학 부근 거래처', 0, TRUE),
    (3, '지성문구', '부산대 인근 거래처', 0, TRUE),
    (4, '마트', '야학 부근 거래처', 0, TRUE),
    (5, '지출증빙 테스트 거래처', '지출증빙서류 생성 API 테스트용 거래처', 79000, TRUE);
ALTER SEQUENCE vendors_id_seq RESTART WITH 6;

-- 14-1. Purchased Purchase Request for Expense Document API Test
INSERT INTO purchase_requests (
    id, classroom_id, requested_by, title, content, total_price, status,
    approval_at, approval_by, purchased_at, note, created_at, updated_at
)
VALUES
    (
        1,
        1,
        2,
        '지출증빙서류 API 테스트',
        '지출증빙서류 생성 API 호출을 확인하기 위한 구매 완료 테스트 데이터입니다.',
        21000,
        'PURCHASED',
        '2026-06-25 10:00:00',
        1,
        '2026-06-30 14:00:00',
        '지출증빙서류 생성 테스트용 승인 데이터',
        '2026-06-24 09:00:00',
        '2026-06-30 14:00:00'
    );
ALTER SEQUENCE purchase_requests_id_seq RESTART WITH 2;

INSERT INTO purchase_requests_items (id, purchase_request_id, name, reason, quantity, payment_type)
VALUES
    (1, 1, '교재 제본', '수업 교재 배부용', 1, 'PREPAID'),
    (2, 1, '수업용 문구 세트', '분반 수업 진행용', 1, 'PREPAID'),
    (3, 1, '학생 배부용 파일', '학습 자료 정리용', 1, 'PREPAID'),
    (4, 1, '화이트보드 마커', '교실 판서용', 1, 'PREPAID'),
    (5, 1, '출석부 바인더', '출석 자료 보관용', 1, 'PREPAID'),
    (6, 1, '수업 안내 인쇄물', '학생 안내문 배부용', 1, 'PREPAID');
ALTER SEQUENCE purchase_requests_items_id_seq RESTART WITH 7;

INSERT INTO purchase_request_payment_transactions (id, purchase_request_id, vendor_id, amount, receipt_file_id)
VALUES
    (1, 1, 5, 12000, NULL),
    (2, 1, 5, 9000, NULL);
ALTER SEQUENCE purchase_request_payment_transactions_id_seq RESTART WITH 3;

INSERT INTO purchase_request_payment_transaction_item_names (transaction_id, sort_order, item_name)
VALUES
    (1, 0, '교재 제본'),
    (1, 1, '수업용 문구 세트'),
    (2, 0, '학생 배부용 파일'),
    (2, 1, '화이트보드 마커'),
    (2, 2, '출석부 바인더'),
    (2, 3, '수업 안내 인쇄물');

INSERT INTO vendor_balance_histories (
    id, vendor_id, type, amount, balance_after, memo, receipt_file_id, purchase_request_id,
    created_by, occurred_at, is_deleted, created_at, updated_at
)
VALUES
    (
        1, 5, 'CHARGE', 100000, 100000, '지출증빙서류 테스트 거래처 초기 충전',
        NULL, NULL, 1, '2026-06-24 09:30:00', FALSE, '2026-06-24 09:30:00', '2026-06-24 09:30:00'
    ),
    (
        2, 5, 'DEDUCT', 21000, 79000, '지출증빙서류 API 테스트 구매 완료',
        NULL, 1, 1, '2026-06-30 14:00:00', FALSE, '2026-06-30 14:00:00', '2026-06-30 14:00:00'
    );
ALTER SEQUENCE vendor_balance_histories_id_seq RESTART WITH 3;

-- 15. Students
INSERT INTO students (id, name, phone_number, description, status)
VALUES
    (1, '이영희', '010-3333-3333', '벚꽃반 기초 문해 학습자', 'ENROLLED'),
    (2, '박민수', '010-4444-4444', '벚꽃반 생활 문해 학습자', 'ENROLLED'),
    (3, '김영자', '010-5555-3333', '개나리반 문해 복습 학습자', 'ENROLLED'),
    (4, '최순덕', '010-5555-4444', '개나리반 생활 수학 학습자', 'ENROLLED'),
    (5, '오민호', '010-5555-5555', '국화반 신규 배정 대기 학습자', 'ENROLLED'),
    (6, '장복희', '010-5555-6666', '주말 스마트폰반 학습자', 'ENROLLED');
ALTER SEQUENCE students_id_seq RESTART WITH 7;

-- 15. Student Classrooms
INSERT INTO student_classrooms (id, student_id, classroom_id)
VALUES
    (1, 1, 1),
    (2, 2, 1),
    (3, 3, 2),
    (4, 4, 2),
    (5, 5, 6),
    (6, 6, 8);
ALTER SEQUENCE student_classrooms_id_seq RESTART WITH 7;

-- 15. Daily Student Attendances
INSERT INTO daily_student_attendances (
    id, daily_schedule_id, student_id, status, is_deleted, created_at, updated_at
)
VALUES
    (1, 6, 1, 'PRESENT', FALSE, '2026-05-13 22:00:00', '2026-05-13 22:00:00'),
    (2, 6, 2, 'LATE', FALSE, '2026-05-13 22:00:00', '2026-05-13 22:00:00'),
    (3, 7, 1, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (4, 7, 2, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (5, 8, 3, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (6, 8, 4, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (7, 9, 1, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (8, 9, 2, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (9, 10, 3, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (10, 10, 4, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (11, 1, 1, 'ABSENT', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (12, 1, 2, 'ABSENT', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (13, 2, 3, 'ABSENT', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (14, 2, 4, 'ABSENT', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (15, 3, 1, 'ABSENT', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (16, 3, 2, 'ABSENT', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (17, 4, 3, 'ABSENT', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (18, 4, 4, 'ABSENT', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (19, 5, 6, 'ABSENT', FALSE, '2026-05-20 00:00:00', '2026-05-20 00:00:00'),
    (20, 11, 1, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (21, 11, 2, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (22, 12, 3, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (23, 12, 4, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
    (24, 13, 6, 'ABSENT', FALSE, '2026-06-01 00:00:00', '2026-06-01 00:00:00');
ALTER SEQUENCE daily_student_attendances_id_seq RESTART WITH 25;
