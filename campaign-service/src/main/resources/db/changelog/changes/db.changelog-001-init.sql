--liquibase formatted sql

--changeset codex:campaign-001-init
CREATE TABLE IF NOT EXISTS campaigns (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value DOUBLE PRECISION NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_campaigns_code
    ON campaigns (code);

ALTER TABLE campaigns
    DROP CONSTRAINT IF EXISTS campaigns_discount_type_check;

ALTER TABLE campaigns
    ADD CONSTRAINT campaigns_discount_type_check
    CHECK (discount_type IN ('PERCENTAGE', 'FLAT_AMOUNT'));
