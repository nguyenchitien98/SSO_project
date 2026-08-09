-- init.sql: Tạo các database cho dự án SSO Platform
CREATE DATABASE sso_db;
CREATE DATABASE monolith_db;
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE notification_db;
CREATE DATABASE file_db;

-- Cấp quyền truy cập cho user postgres
GRANT ALL PRIVILEGES ON DATABASE sso_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE monolith_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE user_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE product_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE order_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE file_db TO postgres;
