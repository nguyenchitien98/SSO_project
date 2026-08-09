CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    user_id          UUID NOT NULL REFERENCES user_profiles(id),
    order_code       VARCHAR(50) UNIQUE NOT NULL,
    status           VARCHAR(50) NOT NULL,
    total_amount     NUMERIC(18, 2) NOT NULL,
    shipping_address TEXT,
    notes            TEXT,
    idempotency_key  VARCHAR(255) UNIQUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_code ON orders(order_code);

CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    quantity    INT NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(18, 2) NOT NULL,
    subtotal    NUMERIC(18, 2) NOT NULL
);
