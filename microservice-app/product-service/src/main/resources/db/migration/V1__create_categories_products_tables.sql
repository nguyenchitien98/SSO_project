CREATE TABLE categories (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(100) UNIQUE NOT NULL,
    parent_id BIGINT REFERENCES categories(id)
);

CREATE TABLE products (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    price         NUMERIC(18, 2) NOT NULL,
    stock         INT NOT NULL DEFAULT 0,
    category_id   BIGINT REFERENCES categories(id),
    created_by    UUID NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    version       INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ms_products_category ON products(category_id);
CREATE INDEX idx_ms_products_active ON products(active);
