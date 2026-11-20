ALTER TABLE users ADD COLUMN homeroom_classroom_id BIGINT;

ALTER TABLE users
    ADD CONSTRAINT fk_users_homeroom_classroom
        FOREIGN KEY (homeroom_classroom_id) REFERENCES classrooms(id);

ALTER TABLE users
    ADD CONSTRAINT uk_users_homeroom_classroom
        UNIQUE (homeroom_classroom_id);

ALTER TABLE users
    ADD CONSTRAINT ck_users_work_assignment_exclusive
        CHECK (department_id IS NULL OR homeroom_classroom_id IS NULL);
