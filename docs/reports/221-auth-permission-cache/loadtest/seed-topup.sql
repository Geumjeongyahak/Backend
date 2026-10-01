-- 테스트 시드(src/test/resources/sql/init_data.sql) 위에, 인증 경로가 읽는 테이블을 dev 행 수에 맞춰 합성 데이터로 채운다.
-- dev(2026-10-01): users 63 · user_credentials 65 · user_permissions 15 · department_permissions 34 · departments 5
-- 실제 사용자 데이터는 쓰지 않는다.
-- 비밀번호는 모두 teacher01 (시드의 teacher01 해시를 재사용).

INSERT INTO users (id, name, primary_email, role, department_id)
SELECT g,
       '부하사용자' || g,
       'lt' || g || '@test.com',
       CASE WHEN g % 10 = 0 THEN 'MANAGER' WHEN g % 7 = 0 THEN 'GUEST' ELSE 'VOLUNTEER' END,
       CASE WHEN g % 7 = 0 THEN NULL ELSE 1 + g % 5 END
FROM generate_series(6, 63) g;

INSERT INTO user_credentials (user_id, provider, credential_email, password_hash, email_verified)
SELECT g, 'LOCAL', 'lt' || g || '@test.com',
       '$2a$12$nsaiXXxEBMV9vNwh23WInekm2WisaINtbtLsP1JXlgHnt9eDqnaRu', TRUE
FROM generate_series(6, 63) g;

-- 구글 자격증명을 함께 가진 사용자 2명 (65 행 맞춤)
INSERT INTO user_credentials (user_id, provider, provider_user_id, credential_email, email_verified)
SELECT g, 'GOOGLE', 'google-' || g, 'lt' || g || '@gmail.com', TRUE
FROM generate_series(6, 7) g;

-- 개인 권한 8행 (15 행 맞춤)
INSERT INTO user_permissions (user_id, permission_code)
SELECT g, 'channel:write:' || (1 + g % 7)
FROM generate_series(10, 17) g;

ALTER TABLE users ALTER COLUMN id RESTART WITH 64;
SELECT setval(pg_get_serial_sequence('user_credentials', 'id'), (SELECT max(id) FROM user_credentials));
