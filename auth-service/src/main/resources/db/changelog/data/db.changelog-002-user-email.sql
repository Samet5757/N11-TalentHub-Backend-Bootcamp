--liquibase formatted sql

--changeset codex:auth-seed-002-user-email
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email VARCHAR(255);

UPDATE users
SET email = CONCAT(username, '@example.local')
WHERE email IS NULL OR email = '';

ALTER TABLE users
    ALTER COLUMN email SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS users_email_key
    ON users (email);
