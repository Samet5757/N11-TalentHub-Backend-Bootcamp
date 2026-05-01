--liquibase formatted sql

--changeset codex:payment-001-init
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    payment_intent_id VARCHAR(255) NULL,
    idempotency_key VARCHAR(255) NULL,
    payment_status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_messages (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message_key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL,
    error_message VARCHAR(1000) NULL,
    retry_count INTEGER NOT NULL,
    max_retry INTEGER NOT NULL,
    next_attempt_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS processed_events (
    id BIGSERIAL PRIMARY KEY,
    consumer_group VARCHAR(255) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS dead_letter_events (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message_key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_payment_intent_id
    ON payments (payment_intent_id)
    WHERE payment_intent_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_idempotency_key
    ON payments (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_processed_event
    ON processed_events (consumer_group, event_id);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_attempt
    ON outbox_messages (status, next_attempt_at, created_at);

ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS payments_payment_status_check;

ALTER TABLE payments
    ADD CONSTRAINT payments_payment_status_check
    CHECK (payment_status IN ('PENDING', 'SUCCESS', 'FAILED'));

ALTER TABLE outbox_messages
    DROP CONSTRAINT IF EXISTS outbox_messages_status_check;

ALTER TABLE outbox_messages
    ADD CONSTRAINT outbox_messages_status_check
    CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'));
