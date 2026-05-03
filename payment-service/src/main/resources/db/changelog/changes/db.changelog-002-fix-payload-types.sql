--liquibase formatted sql

--changeset codex:payment-002-fix-payload-types
ALTER TABLE IF EXISTS public.dead_letter_events
    ALTER COLUMN payload TYPE TEXT USING payload::text;

ALTER TABLE IF EXISTS public.outbox_messages
    ALTER COLUMN payload TYPE TEXT USING payload::text;
