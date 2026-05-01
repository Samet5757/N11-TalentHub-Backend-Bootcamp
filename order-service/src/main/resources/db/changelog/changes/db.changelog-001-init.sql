--liquibase formatted sql

--changeset codex:order-001-init
--validCheckSum: 9:e6126b366cd9d7ff0ae96ec8409d90f1
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    total_amount DOUBLE PRECISION NOT NULL,
    discount_amount DOUBLE PRECISION NOT NULL,
    final_amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_processed_event
    ON processed_events (consumer_group, event_id);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_attempt
    ON outbox_messages (status, next_attempt_at, created_at);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_status_next_attempt
    ON order_notification_outbox (status, next_attempt_at, created_at);

CREATE UNIQUE INDEX IF NOT EXISTS uk_notification_outbox_order
    ON order_notification_outbox (order_id);

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS orders_status_check;

ALTER TABLE orders
    ADD CONSTRAINT orders_status_check
    CHECK (status IN (
        'CREATED',
        'PENDING',
        'INVENTORY_RESERVED',
        'PAYMENT_PENDING',
        'PAYMENT_AUTHORIZED',
        'PAID',
        'APPROVED',
        'SHIPPED',
        'DELIVERED',
        'COMPLETED',
        'FAILED',
        'CANCELLED'
    ));

ALTER TABLE outbox_messages
    DROP CONSTRAINT IF EXISTS outbox_messages_status_check;

ALTER TABLE outbox_messages
    ADD CONSTRAINT outbox_messages_status_check
    CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'));

ALTER TABLE order_notification_outbox
    DROP CONSTRAINT IF EXISTS order_notification_outbox_status_check;

ALTER TABLE order_notification_outbox
    ADD CONSTRAINT order_notification_outbox_status_check
    CHECK (status IN ('PENDING', 'SENT', 'FAILED'));
