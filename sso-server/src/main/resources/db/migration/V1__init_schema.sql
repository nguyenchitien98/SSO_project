-- V1__init_schema.sql: Khởi tạo database schema cho sso_db
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================
-- Users Table
-- =============================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  UNIQUE NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    locked        BOOLEAN NOT NULL DEFAULT FALSE,
    locked_reason VARCHAR(255),
    failed_login_attempts INT NOT NULL DEFAULT 0,
    totp_secret   VARCHAR(255),
    totp_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- =============================================
-- Roles và Permissions Tables
-- =============================================
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    resource    VARCHAR(50) NOT NULL,
    action      VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================
-- Junction Tables (Many-to-Many)
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
-- OAuth2 Clients
-- =============================================
CREATE TABLE oauth_clients (
    id                             VARCHAR(100) PRIMARY KEY,
    client_secret                  VARCHAR(255),
    client_name                    VARCHAR(100) NOT NULL,
    grant_types                    TEXT NOT NULL,
    redirect_uris                  TEXT,
    scopes                         TEXT NOT NULL,
    access_token_ttl_seconds       INT NOT NULL DEFAULT 900,
    refresh_token_ttl_seconds      INT NOT NULL DEFAULT 604800,
    require_pkce                   BOOLEAN NOT NULL DEFAULT TRUE,
    require_authorization_consent  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================
-- Sessions và Refresh Tokens
-- =============================================
CREATE TABLE sessions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id      VARCHAR(100) NOT NULL REFERENCES oauth_clients(id),
    ip_address     VARCHAR(45),
    user_agent     TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked        BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);

CREATE TABLE refresh_tokens (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash     VARCHAR(255) UNIQUE NOT NULL,
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id     UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    family_id      UUID NOT NULL,
    client_id      VARCHAR(100) NOT NULL,
    revoked        BOOLEAN NOT NULL DEFAULT FALSE,
    revoke_reason  VARCHAR(100),
    issued_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- =============================================
-- Audit Logs
-- =============================================
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID REFERENCES users(id),
    client_id   VARCHAR(100),
    action      VARCHAR(100) NOT NULL,
    resource    VARCHAR(50),
    resource_id VARCHAR(100),
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    details     JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
