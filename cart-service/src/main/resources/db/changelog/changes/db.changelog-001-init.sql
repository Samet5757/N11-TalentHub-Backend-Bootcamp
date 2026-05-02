--liquibase formatted sql

--changeset codex:cart-001-init
CREATE TABLE IF NOT EXISTS carts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    total_amount DOUBLE PRECISION NOT NULL,
    coupon_code VARCHAR(255) NULL,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_carts_customer_id
    ON carts (customer_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id
    ON cart_items (cart_id);
