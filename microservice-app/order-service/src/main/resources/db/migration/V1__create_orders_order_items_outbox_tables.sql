CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    user_id          UUID NOT NULL,
    order_code       VARCHAR(50) UNIQUE NOT NULL,
    status           VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount     NUMERIC(18, 2) NOT NULL,
    idempotency_key  VARCHAR(255) UNIQUE,
    version          INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT NOT NULL REFERENCES orders(id),
    product_id   BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity     INT NOT NULL,
    unit_price   NUMERIC(18, 2) NOT NULL,
    subtotal     NUMERIC(18, 2) NOT NULL
);

CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type   VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload      JSONB NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count  INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sent_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_status ON outbox_events(status, created_at);
