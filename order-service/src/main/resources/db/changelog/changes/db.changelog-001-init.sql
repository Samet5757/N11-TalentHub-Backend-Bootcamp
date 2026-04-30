--liquibase formatted sql

--changeset codex:order-001-init
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_processed_event
    ON processed_events (consumer_group, event_id);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_attempt
    ON outbox_messages (status, next_attempt_at, created_at);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'orders_status_check'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT orders_status_check
            CHECK (status IN (
                'PENDING',
                'INVENTORY_RESERVED',
                'PAYMENT_PENDING',
                'PAYMENT_AUTHORIZED',
                'APPROVED',
                'SHIPPED',
                'DELIVERED',
                'COMPLETED',
                'FAILED',
                'CANCELLED'
            ));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'outbox_messages_status_check'
    ) THEN
        ALTER TABLE outbox_messages
            ADD CONSTRAINT outbox_messages_status_check
            CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'));
    END IF;
END $$;
