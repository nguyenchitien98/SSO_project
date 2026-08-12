-- V13__set_order_service_client_secret.sql
-- Thiết lập mật khẩu bảo mật (client_secret) cho order-service và payment-service.
-- Mật khẩu giải mã dạng thô tương ứng với hash BCrypt này là: "admin123"

UPDATE oauth_clients
SET client_secret = '$2a$12$6t3rQyS/m80k2YfR7v1aOuX.i4s0cIEXtB6K.yBsp0F4tqL/L0wUq'
WHERE id IN ('order-service', 'payment-service');
