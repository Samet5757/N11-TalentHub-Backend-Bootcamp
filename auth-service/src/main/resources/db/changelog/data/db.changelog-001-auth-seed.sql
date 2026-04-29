--liquibase formatted sql

--changeset codex:auth-seed-001
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

INSERT INTO users (username, password, role)
VALUES
    ('customer1', 'pass123', 'CUSTOMER'),
    ('seller1', 'pass123', 'SELLER'),
    ('admin1', 'pass123', 'ADMIN')
ON CONFLICT (username) DO NOTHING;
