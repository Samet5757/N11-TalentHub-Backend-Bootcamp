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
    ('customer1', '$2y$10$zBIBrCqK4TYbKXJzUe5UEeCVp/npgdfZZ2Ms3o8h6WyB65HPWfp7u', 'CUSTOMER'),
    ('seller1', '$2y$10$zBIBrCqK4TYbKXJzUe5UEeCVp/npgdfZZ2Ms3o8h6WyB65HPWfp7u', 'SELLER'),
    ('admin1', '$2y$10$zBIBrCqK4TYbKXJzUe5UEeCVp/npgdfZZ2Ms3o8h6WyB65HPWfp7u', 'ADMIN')
ON CONFLICT (username) DO NOTHING;

UPDATE users
SET password = '$2y$10$zBIBrCqK4TYbKXJzUe5UEeCVp/npgdfZZ2Ms3o8h6WyB65HPWfp7u'
WHERE username IN ('customer1', 'seller1', 'admin1')
  AND password = 'pass123';
