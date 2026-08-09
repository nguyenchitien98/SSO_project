# SSO Platform - Hướng Dẫn Chạy Project (Run Guide)

---

## 1. Yêu Cầu Hệ Thống

| Phần mềm | Version | Ghi chú |
|---|---|---|
| Java | 21+ (LTS) | Khuyên dùng GraalVM hoặc Eclipse Temurin |
| Maven | 3.9+ | Wrapper có sẵn `./mvnw` |
| Docker | 24+ | Docker Desktop hoặc Docker Engine |
| Docker Compose | v2 | Dùng `docker compose` (không phải `docker-compose`) |
| Git | 2.40+ | |

---

## 2. Clone & Setup

```bash
# Clone repository
git clone https://github.com/[your-username]/sso-platform.git
cd sso-platform

# Cấu hình hosts file (local development)
# Thêm vào /etc/hosts (Linux/Mac) hoặc C:\Windows\System32\drivers\etc\hosts (Windows):
# 127.0.0.1  sso-server.local
```

---

## 3. Khởi Động Infrastructure

```bash
# Bước 1: Khởi động toàn bộ infrastructure (PostgreSQL, Redis, Kafka, Monitoring)
docker compose -f infrastructure/docker-compose.infra.yml up -d

# Kiểm tra status
docker compose -f infrastructure/docker-compose.infra.yml ps

# Kết quả mong đợi:
# ✅ postgres      — Up (healthy)   :5432
# ✅ redis         — Up (healthy)   :6379
# ✅ kafka         — Up (healthy)   :9092
# ✅ kafka-ui      — Up             :8080
# ✅ minio         — Up (healthy)   :9000 (API), :9001 (Console)
# ✅ prometheus    — Up             :9090
# ✅ grafana       — Up             :3000
# ✅ loki          — Up             :3100

# Bước 2: Kiểm tra databases đã được tạo
docker exec -it sso-postgres psql -U postgres -c "\l"
# Phải thấy: sso_db, monolith_db, user_db, product_db, order_db, payment_db, notification_db, file_db
```

---

## 4. Chạy SSO Server

```bash
cd sso-server

# Chạy với Maven (dev mode)
./mvnw spring-boot:run

# Hoặc build và chạy JAR
./mvnw clean package -DskipTests
java -jar target/sso-server-1.0.0.jar

# Kiểm tra SSO Server
curl http://localhost:9000/.well-known/openid-configuration | jq .
# Phải thấy JSON với issuer, authorization_endpoint, token_endpoint, jwks_uri...

curl http://localhost:9000/oauth2/jwks | jq .
# Phải thấy public key(s) trong JWKS format
```

**SSO Server Health:** `http://localhost:9000/actuator/health`

---

## 5. Chạy Monolith App

```bash
cd monolith-app

# Cấu hình SSO Server URL trong application.yml:
# spring.security.oauth2.client.provider.sso.issuer-uri: http://localhost:9000

./mvnw spring-boot:run

# Test login flow:
# 1. Mở browser: http://localhost:8080/api/products
# 2. Redirect về http://localhost:9000/login
# 3. Login: admin / admin123
# 4. Redirect về Monolith với access token
# 5. Gọi API với JWT: curl http://localhost:8080/api/products -H "Authorization: Bearer <token>"
```

**Monolith Health:** `http://localhost:8080/actuator/health`

---

## 6. Chạy Microservice App

```bash
# Bước 1: Khởi động Config Server
cd microservice-app/config-server
./mvnw spring-boot:run
# Config Server chạy tại :8888

# Bước 2: Khởi động Eureka Server
cd ../eureka-server
./mvnw spring-boot:run
# Eureka Dashboard: http://localhost:8761

# Bước 3: Khởi động API Gateway
cd ../api-gateway
./mvnw spring-boot:run
# Gateway chạy tại :8090

# Bước 4: Khởi động các services (mỗi service 1 terminal)
cd ../user-service    && ./mvnw spring-boot:run &
cd ../product-service && ./mvnw spring-boot:run &
cd ../order-service   && ./mvnw spring-boot:run &
cd ../payment-service && ./mvnw spring-boot:run &
cd ../notification-service && ./mvnw spring-boot:run &
cd ../file-service    && ./mvnw spring-boot:run &

# Kiểm tra tất cả services đã đăng ký Eureka:
# http://localhost:8761 → thấy tất cả services
```

---

## 7. Test API Flow

### 7.1 Lấy Access Token

```bash
# Authorization Code Flow (với PKCE)
# Bước 1: Mở browser và authorize
# http://localhost:9000/oauth2/authorize?
#   client_id=microservice-gateway&
#   response_type=code&
#   redirect_uri=http://localhost:3001/callback&
#   scope=openid profile email&
#   code_challenge=<pkce-challenge>&
#   code_challenge_method=S256

# Bước 2: Login và nhận Authorization Code

# Bước 3: Exchange code lấy token (via PKCE code_verifier)
curl -X POST http://localhost:9000/oauth2/token \
  -d "grant_type=authorization_code" \
  -d "code=<authorization_code>" \
  -d "redirect_uri=http://localhost:3001/callback" \
  -d "client_id=microservice-gateway" \
  -d "code_verifier=<pkce_code_verifier>"
```

### 7.2 Gọi API qua Gateway

```bash
TOKEN="<access_token>"

# Product API (Public - không cần auth)
curl http://localhost:8090/api/products

# Product API (Cần auth)
curl http://localhost:8090/api/products \
  -H "Authorization: Bearer $TOKEN"

# Order API
curl -X POST http://localhost:8090/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-001" \
  -d '{"items": [{"productId": 1, "quantity": 2}]}'
```

### 7.3 Test Phân Quyền

```bash
# Test USER không được delete product
USER_TOKEN="<user_access_token>"
curl -X DELETE http://localhost:8090/api/products/1 \
  -H "Authorization: Bearer $USER_TOKEN"
# Kết quả: HTTP 403 Forbidden

# Test ADMIN được delete product
ADMIN_TOKEN="<admin_access_token>"
curl -X DELETE http://localhost:8090/api/products/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# Kết quả: HTTP 204 No Content
```

---

## 8. Monitoring & Dashboards

| Service | URL | Credentials |
|---|---|---|
| Eureka Dashboard | http://localhost:8761 | Không cần |
| Kafka UI | http://localhost:8080 | Không cần |
| Prometheus | http://localhost:9090 | Không cần |
| Grafana | http://localhost:3000 | admin / admin |
| SSO Admin | http://localhost:9000/admin | admin / admin123 |

---

## 9. Chạy Full Stack với Docker Compose

```bash
# Build tất cả services
./mvnw clean package -DskipTests

# Khởi động toàn bộ hệ thống
docker compose -f infrastructure/docker-compose.full.yml up --build

# Kiểm tra
docker compose ps

# Xem logs của service cụ thể
docker compose logs -f order-service

# Dừng tất cả
docker compose down

# Dừng và xóa volumes (reset data)
docker compose down -v
```

---

## 10. Chạy Tests

```bash
# Unit Tests (không cần Docker)
./mvnw test

# Integration Tests (cần Docker để chạy Testcontainers)
./mvnw verify

# Chạy test cho một service cụ thể
./mvnw test -pl monolith-app

# Chạy một test class cụ thể
./mvnw test -pl monolith-app -Dtest=OrderServiceSecurityTest

# Load test với k6 (cần cài k6: https://k6.io/docs/get-started/installation/)
k6 run tests/load-tests/order-creation.js
```

---

## 11. Tài Khoản Mặc Định Sau Seed

| Username | Password | Role | Mô tả |
|---|---|---|---|
| admin | admin123 | ADMIN | Toàn quyền hệ thống |
| manager1 | Test@1234 | MANAGER | Quản lý sản phẩm & đơn hàng |
| staff1 | Test@1234 | STAFF | Nhân viên xử lý đơn |
| auditor1 | Test@1234 | AUDITOR | Chỉ đọc |
| user1 | Test@1234 | USER | Khách hàng |
| support1 | Test@1234 | SUPPORT | Hỗ trợ khách hàng |
