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
