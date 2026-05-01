--liquibase formatted sql

--changeset codex:auth-seed-001
--validCheckSum: 9:813bae2da17dcd2b769f7aef80f98d00
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL
);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email VARCHAR(255);

UPDATE users
SET email = CONCAT(username, '@example.local')
WHERE email IS NULL OR email = '';

ALTER TABLE users
    ALTER COLUMN email SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'users_email_key'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT users_email_key UNIQUE (email);
    END IF;
END
$$;

INSERT INTO users (username, password, email, role)
VALUES
    ('customer1', '$2y$10$zBIBrCqK4TYbKXJzUe5UEeCVp/npgdfZZ2Ms3o8h6WyB65HPWfp7u', 'customer1@example.local', 'CUSTOMER'),
    ('seller1', '$2y$10$zBIBrCqK4TYbKXJzUe5UEeCVp/npgdfZZ2Ms3o8h6WyB65HPWfp7u', 'seller1@example.local', 'SELLER'),
    ('admin1', '$2y$10$zBIBrCqK4TYbKXJzUe5UEeCVp/npgdfZZ2Ms3o8h6WyB65HPWfp7u', 'admin1@example.local', 'ADMIN')
ON CONFLICT (username) DO NOTHING;

UPDATE users
SET password = '$2y$10$zBIBrCqK4TYbKXJzUe5UEeCVp/npgdfZZ2Ms3o8h6WyB65HPWfp7u'
WHERE username IN ('customer1', 'seller1', 'admin1')
  AND password = 'pass123';
