DROP TABLE IF EXISTS absence_requests;
DROP TABLE IF EXISTS lesson_exchange_requests;
DROP TABLE IF EXISTS purchase_requests;
DROP TABLE IF EXISTS subject_exchange_requests;
DROP TABLE IF EXISTS department_permissions;
DROP TABLE IF EXISTS user_permissions;
DROP TABLE IF EXISTS user_department;
DROP TABLE IF EXISTS lessons;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS classrooms;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(512),
    phone_number VARCHAR(20),
    client_id VARCHAR(512),
    provider_type VARCHAR(20),
    role VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE user_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (department_id) REFERENCES departments(id)
);
CREATE TABLE user_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    granter_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE department_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id)
);

CREATE TABLE classrooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    start_at DATE NOT NULL,
    end_at DATE NOT NULL,
    times INTEGER NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    period INTEGER NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (class_id) REFERENCES classrooms(id),
    FOREIGN KEY (teacher_id) REFERENCES users(id)
);

CREATE TABLE lessons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    attendance VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (teacher_id) REFERENCES users(id)
);

CREATE TABLE absence_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    approval_at DATETIME,
    approval_by BIGINT,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (lesson_id) REFERENCES lessons(id),
    FOREIGN KEY (requested_by) REFERENCES users(id),
    FOREIGN KEY (approval_by) REFERENCES users(id)
);

CREATE TABLE lesson_exchange_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    approval_at DATETIME,
    approval_by BIGINT,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (lesson_id) REFERENCES lessons(id),
    FOREIGN KEY (requested_by) REFERENCES users(id),
    FOREIGN KEY (approval_by) REFERENCES users(id)
);

CREATE TABLE purchase_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    price BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    approval_at DATETIME,
    approval_by BIGINT,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (requested_by) REFERENCES users(id),
    FOREIGN KEY (approval_by) REFERENCES users(id)
);

CREATE TABLE subject_exchange_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    approval_at DATETIME,
    approval_by BIGINT,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (requested_by) REFERENCES users(id),
    FOREIGN KEY (approval_by) REFERENCES users(id)
);