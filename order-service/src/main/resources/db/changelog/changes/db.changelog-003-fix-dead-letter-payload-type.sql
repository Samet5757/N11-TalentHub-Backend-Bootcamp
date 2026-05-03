--liquibase formatted sql

--changeset codex:order-003-fix-dead-letter-payload-type
ALTER TABLE IF EXISTS public.dead_letter_events
    ALTER COLUMN payload TYPE TEXT USING payload::text;
