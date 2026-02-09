-- username : admin1234, password : admin1234
INSERT INTO users (id, username, name, password_hash) VALUES
    (1, 'admin1234', 'Administrator', '$2a$10$A0Av/dPBUz5uoDmp0Z/2S.dsMzOWFL5gLK7CrXmQp6Rw2vqWulapi');

INSERT INTO roles (id, name, description) VALUES
    (1, 'ADMIN', '기본 권한, 모든 권한을 가진 관리자'),
    (2, 'MANAGER', '기본 권한, 일부 관리 권한을 가진 매니저'),
    (3, 'VOLUNTEER', '기본 권한, 제한된 권한을 가진 기관의 자원 봉사자'),
    (4, 'GUEST', '기본 권한, 읽기 전용 접근 권한을 가진 손님');

INSERT INTO roles (id, name, description) VALUES
    (1001, 'DEPT_ADMIN', '부서 권한, 총무 부서의 관리 권한'),
    (1002, 'DEPT_HR', '부서 권한, 인사 부서의 관리 권한'),
    (1003, 'DEPT_FINANCE', '부서 권한, 재무 부서의 관리 권한'),
    (1004, 'DEPT_IT', '부서 권한, IT 부서의 관리 권한'),
    (1005, 'DEPT_MARKETING', '부서 권한, 마케팅 부서의 관리 권한'),
    (1006, 'DEPT_ACADEMIC', '부서 권한, 학사 부서의 관리 권한');

INSERT INTO roles (id, name, description) VALUES
    (2001, 'TEACHER', '직책 권한, 교사 직책의 권한');

INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 1);  -- Assign ADMIN role to the admin user