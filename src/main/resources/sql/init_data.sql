INSERT INTO users (id, name, email, password_hash, role, provider_type, created_at, updated_at)
VALUES (1,'Admin','admin@test.com','admin','SUPER_ADMIN','EMAIL',NOW(),NOW());

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

INSERT INTO user_permissions (user_id, permission_id, granter_type, created_at, updated_at)
VALUES (1, 100, 'USER', NOW(), NOW());