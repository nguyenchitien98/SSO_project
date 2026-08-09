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
