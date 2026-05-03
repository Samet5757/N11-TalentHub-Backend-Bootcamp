--liquibase formatted sql

--changeset codex:order-004-fix-outbox-payload-type
ALTER TABLE IF EXISTS public.outbox_messages
    ALTER COLUMN payload TYPE TEXT USING payload::text;
