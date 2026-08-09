CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE file_metadata (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    mime_type    VARCHAR(100) NOT NULL,
    file_size    BIGINT NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_file_user_id ON file_metadata(user_id);
