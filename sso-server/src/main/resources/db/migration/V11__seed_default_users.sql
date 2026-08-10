-- V11: Seed default users (admin / user)
-- Hashed password for admin123/user123 is: $2a$12$6t3rQyS/m80k2YfR7v1aOuX.i4s0cIEXtB6K.yBsp0F4tqL/L0wUq

INSERT INTO users (id, username, email, password_hash, enabled, locked, failed_login_attempts, totp_enabled)
VALUES ('a1c29e64-28b9-4c3e-8d99-5f210d0f41c3', 'admin', 'admin@sso.com', '$2a$12$6t3rQyS/m80k2YfR7v1aOuX.i4s0cIEXtB6K.yBsp0F4tqL/L0wUq', true, false, 0, false)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (id, username, email, password_hash, enabled, locked, failed_login_attempts, totp_enabled)
VALUES ('b2d39f75-39c0-5d4f-9e0a-6f321e1a52d4', 'user', 'user@sso.com', '$2a$12$6t3rQyS/m80k2YfR7v1aOuX.i4s0cIEXtB6K.yBsp0F4tqL/L0wUq', true, false, 0, false)
ON CONFLICT (username) DO NOTHING;

-- Map roles
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'user' AND r.name = 'USER'
ON CONFLICT (user_id, role_id) DO NOTHING;
