CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id),
    amount          NUMERIC(18, 2) NOT NULL,
    method          VARCHAR(50) NOT NULL,
    status          VARCHAR(50) NOT NULL,
    transaction_ref VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
