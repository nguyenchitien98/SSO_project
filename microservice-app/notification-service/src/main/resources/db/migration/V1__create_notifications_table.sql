CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT NOT NULL,
    type        VARCHAR(50) NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
