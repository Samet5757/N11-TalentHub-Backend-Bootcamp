--liquibase formatted sql

--changeset codex:seller-001-init
CREATE TABLE IF NOT EXISTS sellers (
    id BIGSERIAL PRIMARY KEY,
    store_name VARCHAR(255) NOT NULL,
    tax_number VARCHAR(255) NOT NULL,
    rating_average DOUBLE PRECISION NOT NULL,
    is_official_store BOOLEAN NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sellers_tax_number
    ON sellers (tax_number);
