# SSO Platform - Database Schema Đầy Đủ

File này chứa toàn bộ SQL schema cho các databases của dự án SSO Platform.

---

## 1. SSO Server Database (`sso_db`)

```sql
-- =============================================
-- V1: Users Table
-- =============================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  UNIQUE NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,           -- BCrypt/Argon2 hash
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    locked        BOOLEAN NOT NULL DEFAULT FALSE,
    locked_reason VARCHAR(255),                    -- Lý do bị khóa (brute force, admin action...)
    failed_login_attempts INT NOT NULL DEFAULT 0,
    totp_secret   VARCHAR(255),                    -- Mã secret kích hoạt 2FA (mã hóa AES)
    totp_enabled  BOOLEAN NOT NULL DEFAULT FALSE,  -- Trạng thái kích hoạt 2FA
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- =============================================
-- V2: Roles và Permissions Tables
-- =============================================
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) UNIQUE NOT NULL,   -- ADMIN, MANAGER, STAFF, AUDITOR, USER, SUPPORT
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) UNIQUE NOT NULL,  -- PRODUCT_READ, ORDER_CREATE...
    description VARCHAR(255),
    resource    VARCHAR(50) NOT NULL,          -- PRODUCT, ORDER, USER, PAYMENT...
    action      VARCHAR(50) NOT NULL,          -- READ, CREATE, UPDATE, DELETE, REFUND...
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================
-- V3: Junction Tables (Many-to-Many)
-- =============================================
CREATE TABLE user_roles (
    user_id     UUID   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    assigned_by UUID   REFERENCES users(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- =============================================
-- V4: OAuth2 Clients
-- =============================================
CREATE TABLE oauth_clients (
    id                             VARCHAR(100) PRIMARY KEY,   -- client_id: monolith-web, order-service...
    client_secret                  VARCHAR(255),               -- Hashed
    client_name                    VARCHAR(100) NOT NULL,
    grant_types                    TEXT NOT NULL,              -- authorization_code, client_credentials...
    redirect_uris                  TEXT,                       -- Comma-separated URIs
    scopes                         TEXT NOT NULL,              -- openid profile email payment:write...
    access_token_ttl_seconds       INT NOT NULL DEFAULT 900,   -- 15 phút
    refresh_token_ttl_seconds      INT NOT NULL DEFAULT 604800, -- 7 ngày
    require_pkce                   BOOLEAN NOT NULL DEFAULT TRUE,
    require_authorization_consent  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================
-- V5: Sessions và Refresh Tokens
-- =============================================
CREATE TABLE sessions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id      VARCHAR(100) NOT NULL REFERENCES oauth_clients(id),
    ip_address     VARCHAR(45),        -- IPv4 hoặc IPv6
    user_agent     TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked        BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);

CREATE TABLE refresh_tokens (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash     VARCHAR(255) UNIQUE NOT NULL,   -- Hash của refresh token (không lưu plain text)
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id     UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    family_id      UUID NOT NULL,                 -- Token family để detect replay attack
    client_id      VARCHAR(100) NOT NULL,
    revoked        BOOLEAN NOT NULL DEFAULT FALSE,
    revoke_reason  VARCHAR(100),                  -- USED, REPLAY_DETECTED, LOGOUT, ADMIN_REVOKE
    issued_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- =============================================
-- V6: Audit Logs
-- =============================================
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID REFERENCES users(id),     -- NULL nếu action chưa biết user (login fail)
    client_id   VARCHAR(100),
    action      VARCHAR(100) NOT NULL,          -- LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, TOKEN_REFRESH...
    resource    VARCHAR(50),                    -- USER, ROLE, PERMISSION, SESSION, TOKEN
    resource_id VARCHAR(100),                  -- ID của resource bị tác động
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    details     JSONB,                          -- Thông tin bổ sung (ví dụ: old role, new role)
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- =============================================
-- V7: Seed Data — Default Roles và Permissions
-- =============================================
INSERT INTO roles (name, description) VALUES
    ('ADMIN',    'Toàn quyền hệ thống'),
    ('MANAGER',  'Quản lý sản phẩm và đơn hàng'),
    ('STAFF',    'Nhân viên xử lý đơn hàng'),
    ('AUDITOR',  'Chỉ xem, không thay đổi'),
    ('USER',     'Khách hàng thông thường'),
    ('SUPPORT',  'Nhân viên hỗ trợ khách hàng');

INSERT INTO permissions (name, resource, action, description) VALUES
    ('USER_READ',        'USER',    'READ',   'Xem thông tin người dùng'),
    ('USER_CREATE',      'USER',    'CREATE', 'Tạo người dùng mới'),
    ('USER_UPDATE',      'USER',    'UPDATE', 'Cập nhật người dùng'),
    ('USER_DELETE',      'USER',    'DELETE', 'Xóa người dùng'),
    ('PRODUCT_READ',     'PRODUCT', 'READ',   'Xem sản phẩm'),
    ('PRODUCT_CREATE',   'PRODUCT', 'CREATE', 'Tạo sản phẩm mới'),
    ('PRODUCT_UPDATE',   'PRODUCT', 'UPDATE', 'Cập nhật sản phẩm'),
    ('PRODUCT_DELETE',   'PRODUCT', 'DELETE', 'Xóa sản phẩm'),
    ('ORDER_READ',       'ORDER',   'READ',   'Xem đơn hàng'),
    ('ORDER_CREATE',     'ORDER',   'CREATE', 'Tạo đơn hàng mới'),
    ('ORDER_CANCEL',     'ORDER',   'CANCEL', 'Hủy đơn hàng'),
    ('ORDER_REFUND',     'ORDER',   'REFUND', 'Hoàn tiền đơn hàng'),
    ('PAYMENT_READ',     'PAYMENT', 'READ',   'Xem thông tin thanh toán'),
    ('PAYMENT_CREATE',   'PAYMENT', 'CREATE', 'Xử lý thanh toán'),
    ('PAYMENT_REFUND',   'PAYMENT', 'REFUND', 'Hoàn tiền thanh toán'),
    ('AUDIT_READ',       'AUDIT',   'READ',   'Xem audit logs');

-- ADMIN có tất cả permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN';

-- MANAGER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'MANAGER'
AND p.name IN ('USER_READ', 'PRODUCT_READ','PRODUCT_CREATE','PRODUCT_UPDATE',
               'ORDER_READ','ORDER_CANCEL','ORDER_REFUND',
               'PAYMENT_READ','PAYMENT_REFUND','AUDIT_READ');

-- STAFF permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'STAFF'
AND p.name IN ('PRODUCT_READ','PRODUCT_CREATE','PRODUCT_UPDATE','ORDER_READ');

-- AUDITOR permissions (read-only)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'AUDITOR'
AND p.name IN ('USER_READ','PRODUCT_READ','ORDER_READ','PAYMENT_READ','AUDIT_READ');

-- USER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'USER'
AND p.name IN ('PRODUCT_READ','ORDER_READ','ORDER_CREATE');

-- SUPPORT permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPPORT'
AND p.name IN ('USER_READ','ORDER_READ','PAYMENT_READ');

-- Default OAuth2 Clients
INSERT INTO oauth_clients (id, client_name, grant_types, redirect_uris, scopes, require_pkce) VALUES
    ('monolith-web',       'Monolith Web App',    'authorization_code,refresh_token',
     'http://localhost:8080/login/oauth2/code/sso', 'openid profile email', TRUE),
    ('microservice-gateway','Microservice Gateway','authorization_code,refresh_token',
     'http://localhost:3001/callback', 'openid profile email', TRUE),
    ('order-service',      'Order Service',       'client_credentials', NULL, 'payment:write', FALSE),
    ('payment-service',    'Payment Service',     'client_credentials', NULL, 'order:read', FALSE),
    ('notification-service','Notification Service','client_credentials', NULL, 'user:read', FALSE);
```

---

## 2. Monolith Database (`monolith_db`)

```sql
-- V1: User Profiles
CREATE TABLE user_profiles (
    id           UUID PRIMARY KEY,              -- FK = SSO user ID (UUID)
    display_name VARCHAR(100),
    phone        VARCHAR(20),
    avatar_url   VARCHAR(500),
    address      TEXT,
    preferences  JSONB DEFAULT '{}',
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- V2: Products
CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price       NUMERIC(18, 2) NOT NULL CHECK (price >= 0),
    stock       INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    category    VARCHAR(100),
    image_url   VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_by  UUID NOT NULL REFERENCES user_profiles(id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_active ON products(active);

-- V3: Orders và Order Items
CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    user_id          UUID NOT NULL,               -- SSO user UUID
    order_code       VARCHAR(50) UNIQUE NOT NULL, -- ORD-20260809-001
    status           VARCHAR(50) NOT NULL,         -- PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    total_amount     NUMERIC(18, 2) NOT NULL,
    shipping_address TEXT,
    notes            TEXT,
    idempotency_key  VARCHAR(255) UNIQUE,          -- Chống duplicate order
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
    product_name VARCHAR(255) NOT NULL,           -- Snapshot tên sản phẩm tại thời điểm đặt
    quantity    INT NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(18, 2) NOT NULL,
    subtotal    NUMERIC(18, 2) NOT NULL
);

-- V4: Payments
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id),
    amount          NUMERIC(18, 2) NOT NULL,
    method          VARCHAR(50) NOT NULL,          -- CREDIT_CARD, BANK_TRANSFER, VNPAY, MOMO, VISA, COD, MOCK
    status          VARCHAR(50) NOT NULL,           -- PENDING, COMPLETED, FAILED, REFUNDED
    transaction_ref VARCHAR(255),                  -- Mã giao dịch từ cổng thanh toán
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- V5: Audit Logs
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    UUID NOT NULL,                    -- SSO user UUID thực hiện action
    actor_email VARCHAR(255),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,             -- Product, Order, Payment
    entity_id   VARCHAR(100),
    old_values  JSONB,
    new_values  JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_monolith_audit_actor ON audit_logs(actor_id);
CREATE INDEX idx_monolith_audit_entity ON audit_logs(entity_type, entity_id);
```

---

## 3. Microservice Databases

### Order DB (`order_db`)

```sql
CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    user_id          UUID NOT NULL,
    order_code       VARCHAR(50) UNIQUE NOT NULL,
    status           VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount     NUMERIC(18, 2) NOT NULL,
    idempotency_key  VARCHAR(255) UNIQUE,
    version          INT NOT NULL DEFAULT 0,       -- Optimistic Lock
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

-- Outbox Pattern table
CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type   VARCHAR(100) NOT NULL,           -- ORDER_CREATED, ORDER_CANCELLED
    aggregate_id VARCHAR(100) NOT NULL,           -- Order ID
    payload      JSONB NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    retry_count  INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sent_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_status ON outbox_events(status, created_at);
```

### Product DB (`product_db`)

```sql
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
    version       INT NOT NULL DEFAULT 0,         -- Optimistic Lock
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ms_products_category ON products(category_id);
CREATE INDEX idx_ms_products_active ON products(active);
```

### Payment DB (`payment_db`)

```sql
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    user_id         UUID NOT NULL,
    amount          NUMERIC(18, 2) NOT NULL,
    method          VARCHAR(50) NOT NULL,         -- CREDIT_CARD, BANK_TRANSFER, VNPAY, MOMO, VISA, COD, MOCK
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transaction_ref VARCHAR(255),
    idempotency_key VARCHAR(255) UNIQUE,
    version         INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type   VARCHAR(100) NOT NULL,           -- PAYMENT_COMPLETED, PAYMENT_FAILED
    aggregate_id VARCHAR(100) NOT NULL,
    payload      JSONB NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count  INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sent_at      TIMESTAMP WITH TIME ZONE
);
```

### User DB (`user_db`)

```sql
CREATE TABLE user_profiles (
    id           UUID PRIMARY KEY,              -- = SSO user UUID
    display_name VARCHAR(100),
    avatar_url   VARCHAR(500),
    phone        VARCHAR(20),
    bio          TEXT,
    preferences  JSONB DEFAULT '{}',
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

### Notification DB (`notification_db`)

```sql
CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID NOT NULL,                  -- SSO user UUID nhận thông báo
    title       VARCHAR(255) NOT NULL,
    content     TEXT NOT NULL,
    type        VARCHAR(50) NOT NULL,           -- EMAIL, SMS, IN_APP
    status      VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    error_message TEXT,                         -- Chi tiết lỗi nếu gửi thất bại
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
```

### File DB (`file_db` - file metadata)

```sql
CREATE TABLE file_metadata (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,                 -- SSO user UUID tải lên
    file_name    VARCHAR(255) NOT NULL,         -- Tên file gốc
    storage_path VARCHAR(500) NOT NULL,         -- Đường dẫn lưu trong MinIO bucket
    mime_type    VARCHAR(100) NOT NULL,         -- Loại file (image/png, image/jpeg...)
    file_size    BIGINT NOT NULL,               -- Kích thước file (bytes)
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_file_user_id ON file_metadata(user_id);
```
