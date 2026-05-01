--liquibase formatted sql

--changeset codex:order-002-notification-outbox
CREATE TABLE IF NOT EXISTS order_notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INTEGER NOT NULL,
    max_retry INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    next_attempt_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    error_message VARCHAR(1000) NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_status_next_attempt
    ON order_notification_outbox (status, next_attempt_at, created_at);

CREATE UNIQUE INDEX IF NOT EXISTS uk_notification_outbox_order
    ON order_notification_outbox (order_id);

ALTER TABLE order_notification_outbox
    DROP CONSTRAINT IF EXISTS order_notification_outbox_status_check;

ALTER TABLE order_notification_outbox
    ADD CONSTRAINT order_notification_outbox_status_check
    CHECK (status IN ('PENDING', 'SENT', 'FAILED'));
