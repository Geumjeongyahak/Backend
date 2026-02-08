TRUNCATE TABLE user_permissions, user_department, users, permissions RESTART IDENTITY CASCADE;

INSERT INTO permissions (id, name, created_at, updated_at)
VALUES (100, 'SUPER_ADMIN', NOW(), NOW());
INSERT INTO permissions (id, name, created_at, updated_at)
VALUES (200, 'MANAGE_USERS', NOW(), NOW());
INSERT INTO permissions (id, name, created_at, updated_at)
VALUES (300, 'MANAGE_DEPARTMENTS', NOW(), NOW());
INSERT INTO permissions (id, name, created_at, updated_at)
VALUES (400, 'MANAGE_CLASSROOMS', NOW(), NOW());
INSERT INTO permissions (id, name, created_at, updated_at)
VALUES (500, 'MANAGE_STUDENTS', NOW(), NOW());
INSERT INTO permissions (id, name, created_at, updated_at)
VALUES (600, 'MANAGE_SUBJECTS', NOW(), NOW());
INSERT INTO permissions (id, name, created_at, updated_at)
VALUES (700, 'MANAGE_LESSONS', NOW(), NOW());
INSERT INTO permissions (id, name, created_at, updated_at)
VALUES (800, 'MANAGE_REQUESTS', NOW(), NOW());

-- password : admin
INSERT INTO users (name, email, password_hash, role, provider_type, created_at, updated_at)
VALUES ('Admin','admin@test.com','$2a$12$PJxP3YZmWZcrqDggbqk.jOPtTRynDS/48YewIny.Uo1pq1XFf1mti','MANAGER','LOCAL',NOW(),NOW());

INSERT INTO user_permissions (user_id, permission_id, granter_type, created_at, updated_at)
VALUES (1, 100, 'USER', NOW(), NOW());

-- password : teacher
INSERT INTO users (name, email, password_hash, role, provider_type, created_at, updated_at)
VALUES ('Teacher','teacher@test.com','$2a$12$ZGlUreTxjRxtx7/H5vDY2.TvCN.VZFt/BxISTDG6B5deNqeyE3s16','VOLUNTEER', 'LOCAL',NOW(),NOW());

INSERT INTO user_permissions (user_id, permission_id, granter_type, created_at, updated_at)
VALUES (2, 500, 'USER', NOW(), NOW());