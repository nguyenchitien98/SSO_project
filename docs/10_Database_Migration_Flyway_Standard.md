# SSO Platform - Quy Chuẩn Database Migration (Flyway)

Tài liệu này đặc tả quy định sử dụng Flyway để quản lý phiên bản cơ sở dữ liệu cho toàn bộ services trong **SSO Platform**.

---

## 1. Tại Sao Bắt Buộc Dùng Flyway?

- **Không dùng `ddl-auto=update`**: Hibernate tự sinh SQL dễ gây lock table, mất data silently, không audit được.
- **Tính đồng bộ môi trường**: Dev → CI/CD → Staging → Production có schema giống nhau 100%.
- **Lịch sử thay đổi**: Bảng `flyway_schema_history` lưu ai thay đổi gì, khi nào.
- **Team-safe**: Nhiều người cùng làm, không conflict nếu mỗi người tạo migration file riêng với version tăng dần.

---

## 2. Tổ Chức File Migration

Mỗi service có thư mục migration riêng:

```
[service]/src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__create_indexes.sql
├── V3__seed_default_data.sql
└── V4__add_column_xxx.sql
```

### Quy tắc đặt tên

```
V{version}__{description}.sql

Ví dụ:
V1__create_users_table.sql
V2__create_roles_permissions_tables.sql
V3__add_user_roles_junction.sql
V4__seed_default_roles_and_permissions.sql
V5__add_index_users_email.sql
```

**Quy tắc vàng:**
- **TUYỆT ĐỐI không sửa file cũ** sau khi đã commit lên Git (Flyway checksum fail → app không start)
- Mọi thay đổi schema → tạo file `V{N+1}__...sql` mới
- File Seed data (`V...__seed_...sql`) chỉ chạy 1 lần, dùng `INSERT ... ON CONFLICT DO NOTHING`

---

## 3. Cấu Hình Spring Boot

```yaml
# application.yml — áp dụng cho mọi service có DB
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/${DB_NAME}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Chỉ validate, KHÔNG auto-create/update
    show-sql: false        # true ở local dev nếu cần debug
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    baseline-on-migrate: true   # Tạo baseline nếu DB đã có data trước
    locations: classpath:db/migration
    validate-on-migrate: true   # Validate checksum trước khi migrate
```

---

## 4. Dependencies Maven

```xml
<!-- Parent pom.xml hoặc từng service pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

---

## 5. Quy Trình Thay Đổi Schema

### Thêm cột mới
```sql
-- V5__add_phone_to_user_profiles.sql
ALTER TABLE user_profiles
ADD COLUMN phone VARCHAR(20);
```

### Thêm index
```sql
-- V6__add_index_orders_user_id.sql
CREATE INDEX CONCURRENTLY idx_orders_user_id ON orders(user_id);
-- CONCURRENTLY: không lock table khi tạo index trên bảng lớn
```

### Rename cột (PostgreSQL)
```sql
-- V7__rename_full_name_to_display_name.sql
ALTER TABLE user_profiles
RENAME COLUMN full_name TO display_name;
```

### Seed data (idempotent)
```sql
-- V8__seed_admin_user.sql
INSERT INTO users (id, username, email, password_hash, enabled)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@sso.local',
    '$2a$12$...',  -- BCrypt hash của 'admin123'
    true
)
ON CONFLICT (email) DO NOTHING;  -- Chạy nhiều lần vẫn an toàn
```

---

## 6. Sửa Lỗi Migration Thất Bại (Flyway Repair)

```bash
# Khi Flyway báo lỗi "Migration checksum mismatch" hoặc "Migration failed"

# Bước 1: Sửa file SQL
# Bước 2: Chạy repair để clear failed migration record
mvn flyway:repair -pl [module-name]

# Hoặc kết nối DB và xóa thủ công
DELETE FROM flyway_schema_history WHERE success = false;
```

---

## 7. Kiểm Tra Migration Trước Khi Deploy

```bash
# Xem các migration sẽ chạy (dry-run) — không thực sự thay đổi DB
mvn flyway:info -pl [module-name]

# Validate checksum của tất cả migration đã chạy
mvn flyway:validate -pl [module-name]

# Chạy migrate thủ công
mvn flyway:migrate -pl [module-name]
```
