-- V1__Create_initial_tables.sql

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    ts_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ts_updated TIMESTAMP,
    created_by_user VARCHAR(50),
    created_by_process VARCHAR(50),
    updated_by_user VARCHAR(50),
    updated_by_process VARCHAR(50)
);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    ts_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ts_updated TIMESTAMP,
    created_by_user VARCHAR(50),
    created_by_process VARCHAR(50),
    updated_by_user VARCHAR(50),
    updated_by_process VARCHAR(50)
);

CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    ts_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ts_updated TIMESTAMP,
    created_by_user VARCHAR(50),
    created_by_process VARCHAR(50),
    updated_by_user VARCHAR(50),
    updated_by_process VARCHAR(50)
);

CREATE TABLE user_roles (
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    role_id INT REFERENCES roles(id) ON DELETE CASCADE,
    ts_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ts_updated TIMESTAMP,
    created_by_user VARCHAR(50),
    created_by_process VARCHAR(50),
    updated_by_user VARCHAR(50),
    updated_by_process VARCHAR(50),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_permissions (
    role_id INT REFERENCES roles(id) ON DELETE CASCADE,
    permission_id INT REFERENCES permissions(id) ON DELETE CASCADE,
    ts_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ts_updated TIMESTAMP,
    created_by_user VARCHAR(50),
    created_by_process VARCHAR(50),
    updated_by_user VARCHAR(50),
    updated_by_process VARCHAR(50),
    PRIMARY KEY (role_id, permission_id)
);
