--liquibase formatted sql

--changeset codex:product-messaging-tables-003
CREATE TABLE IF NOT EXISTS outbox_messages (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message_key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    error_message VARCHAR(1000) NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retry INTEGER NOT NULL DEFAULT 5,
    next_attempt_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_attempt
    ON outbox_messages (status, next_attempt_at);

--changeset codex:product-processed-events-003
CREATE TABLE IF NOT EXISTS processed_events (
    id BIGSERIAL PRIMARY KEY,
    consumer_group VARCHAR(255) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_processed_event UNIQUE (consumer_group, event_id)
);

CREATE INDEX IF NOT EXISTS idx_processed_events_event_id
    ON processed_events (event_id);

--changeset codex:product-dead-letter-events-003
CREATE TABLE IF NOT EXISTS dead_letter_events (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message_key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dead_letter_events_created_at
    ON dead_letter_events (created_at);
