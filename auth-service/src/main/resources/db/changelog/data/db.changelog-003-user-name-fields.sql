--liquibase formatted sql

--changeset codex:auth-seed-003-user-name-fields
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);

UPDATE users
SET first_name = COALESCE(NULLIF(first_name, ''), 'System')
WHERE first_name IS NULL OR first_name = '';

UPDATE users
SET last_name = COALESCE(NULLIF(last_name, ''), 'User')
WHERE last_name IS NULL OR last_name = '';

ALTER TABLE users
    ALTER COLUMN first_name SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN last_name SET NOT NULL;
