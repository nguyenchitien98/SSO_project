-- V8__add_admin_client.sql
-- Thêm admin-client cho luồng client_credentials với quyền admin để thao tác trên các APIs quản trị
INSERT INTO oauth_clients (id, client_secret, client_name, grant_types, redirect_uris, scopes, require_pkce) VALUES
    ('admin-client', '$2a$12$6t3rQyS/m80k2YfR7v1aOuX.i4s0cIEXtB6K.yBsp0F4tqL/L0wUq', 'SSO Admin CLI Client', 'client_credentials', NULL, 'admin', FALSE);
