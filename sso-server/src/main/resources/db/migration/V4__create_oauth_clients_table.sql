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
