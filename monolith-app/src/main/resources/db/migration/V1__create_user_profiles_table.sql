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
