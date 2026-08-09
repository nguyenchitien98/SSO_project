# SSO Platform - Tầm Nhìn Dự Án (Project Vision)

Tài liệu này xác định tầm nhìn, phạm vi nghiệp vụ, mục tiêu kỹ thuật và các ràng buộc cốt lõi của hệ thống **SSO Platform**. Đây là kim chỉ nam để lập trình viên và các AI Agents (Gemini, Claude, Cursor) đưa ra các quyết định thiết kế và lập trình nhất quán trong suốt 25 Sprint.

---

## 1. Tuyên Bố Tầm Nhìn (Vision Statement)

Xây dựng một **nền tảng SSO (Single Sign-On) production-grade** bao gồm:

- **SSO Server** — Authorization Server + OpenID Provider chuẩn OAuth2/OIDC.
- **Monolith App** — Ứng dụng Spring Boot nguyên khối tích hợp SSO, phân quyền bằng `@PreAuthorize` + Spring Security.
- **Microservice App** — Hệ thống phân tán API Gateway + 5 microservices, phân quyền bằng JWT validation tại Gateway + Header Propagation (X-User-Id, X-User-Role, X-User-Permissions).

Dự án không chỉ triển khai tính năng, mà còn minh họa rõ ràng **sự khác biệt kiến trúc bảo mật giữa Monolith và Distributed System** — một chủ đề cực kỳ phổ biến trong các buổi phỏng vấn Java Senior/Staff Engineer.

---

## 2. Mục Tiêu Kỹ Thuật (Engineering Objectives)

### 2.1 Mục Tiêu Học Tập & Showcase

- Hiểu sâu **OAuth2 Authorization Code Flow + PKCE** (không tự chế authentication protocol).
- Hiểu sự khác biệt giữa **Authentication** (SSO Server) và **Authorization** (từng ứng dụng).
- Biết cách **Gateway xác thực JWT** từ JWKS endpoint thay vì gọi Auth Service mỗi request.
- Biết cách **strip X-User-\*** header từ phía client và inject trusted headers sau JWT validation.
- Biết cách **service-to-service authentication** bằng OAuth2 Client Credentials flow.
- Hiểu **Refresh Token Rotation**, Token Revocation, Key Rotation với asymmetric RSA keys.
- Triển khai `@PreAuthorize` với method security và resource ownership trong Monolith.
- Triển khai `AuthorizationService` + `CurrentUser` pattern trong Microservice.

### 2.2 Các Tính Năng Kỹ Thuật Cốt Lõi

| Tính năng | Monolith | Microservice |
|---|---|---|
| OAuth2/OIDC Login | ✅ | ✅ |
| Social Login (Google/MS) | ✅ | ✅ |
| 2FA / TOTP | ✅ | ✅ |
| JWT (Access + Refresh Token) | ✅ | ✅ |
| RBAC (Role-Based) | ✅ `@PreAuthorize` | ✅ AuthorizationService |
| ABAC (Resource Ownership) | ✅ SpEL | ✅ Manual check |
| SSO (Session-sharing) | ✅ | ✅ |
| API Gateway + Header propagation | ❌ | ✅ |
| Config Server (Centralized) | ❌ | ✅ |
| Object Storage (MinIO) | ✅ Client | ✅ file-service |
| Service-to-Service auth | ❌ | ✅ Client Credentials |
| Kafka + Outbox | Optional | ✅ |
| Circuit Breaker | ❌ | ✅ Resilience4j |
| Distributed Tracing | ❌ | ✅ OpenTelemetry |
| Brute-force protection | ✅ Redis | ✅ Redis |
| Audit Log | ✅ | ✅ |
| Security Attack Testing | ✅ | ✅ |

---

## 3. Phạm Vi Nghiệp Vụ (Business Scope)

### Nằm trong phạm vi (In-Scope)

Cả Monolith và Microservice đều xây dựng cùng một domain nghiệp vụ:

1. **User Management:** Xem, tạo, cập nhật, vô hiệu hóa tài khoản người dùng.
2. **Product Management:** CRUD sản phẩm với phân quyền theo role.
3. **Order Management:** Tạo, xem, hủy đơn hàng với kiểm soát ownership.
4. **Payment Management:** Xử lý thanh toán giả lập (Mock Sandbox).
5. **Notification:** Gửi email khi có sự kiện (OrderCreated, PaymentCompleted).

### Nằm ngoài phạm vi (Non-Scope)

- **Logistics thực tế:** Chỉ mô phỏng trạng thái giao hàng.
- **Cổng thanh toán thật:** Chỉ dùng Mock/Sandbox.
- **Multi-tenant:** Hệ thống phục vụ một tổ chức duy nhất (single-tenant).
- **Mobile Native App:** Chỉ tập trung Backend API, không xây dựng mobile app.

---

## 4. Ràng Buộc Công Nghệ (Technology Constraints)

### Backend — Chung
- **Java 21 + Spring Boot 3.3.x**
- **PostgreSQL 16** (HikariCP connection pool)
- **Redis 7.x** (Brute-force counter, Rate limiting, Session cache, TOTP cache)
- **Apache Kafka** (Async events — chỉ Microservice App)
- **MinIO Object Storage** (Tương thích S3 API cho file/avatar storage)
- **Flyway** (Database Migration)

### SSO Server
- **Spring Authorization Server 1.3.x** (hỗ trợ 2FA/TOTP và Social Login)
- **RSA-2048 asymmetric key** (sign JWT, expose JWKS endpoint)
- **Spring Security 6.x**

### Monolith App
- **Spring Security 6.x** + `@EnableMethodSecurity`
- **Spring OAuth2 Client** (OIDC Login with SSO Server)
- **Thymeleaf** (Server-side rendered views — optional, hoặc REST API thuần)

### Microservice App
- **Spring Cloud Gateway** (JWT validation via JWKS, Header injection, Rate limiting)
- **Spring Cloud Config Server** (Quản lý cấu hình tập trung)
- **Spring Cloud Netflix Eureka** (Service Discovery)
- **Resilience4j** (Circuit Breaker, Retry, Bulkhead)
- **OpenTelemetry** (Distributed Tracing)
- **Prometheus + Grafana** (Metrics)
- **file-service** (Dịch vụ upload file tích hợp MinIO SDK)

### DevOps & Infrastructure
- **GitLab CI** (Pipeline CI/CD tự động build & test)
- **Docker Compose & Kubernetes** (Hỗ trợ chạy local bằng compose và deploy cụm K8s với Nginx Ingress Controller)

### Build Tool
- **Maven** (Multi-module project)
- **Spotless** (Code formatting — Google Java Style)
- **Lombok** (Boilerplate reduction)
- **MapStruct** (Entity ↔ DTO mapping)

---

## 5. Quy Định Code & Học Tập

- **Comment tiếng Việt bắt buộc:** Mọi class, method, logic phức tạp đều phải có Javadoc và inline comment giải thích bằng tiếng Việt.
- **Không viết code giả:** Không để lại `// TODO`, `return null`, `throw new UnsupportedOperationException()`.
- **Mỗi Sprint phải pass:** Sau mỗi Sprint, hệ thống phải compile, pass test và khởi chạy được qua Docker Compose.
- **Kiến trúc phải nhất quán:** Mọi quyết định kiến trúc phải có giải thích tại sao (ADR).

---

## 6. Sơ Đồ Tổng Thể Hệ Thống

```
                         ┌─────────────────┐
                         │    Browser /    │
                         │    Postman      │
                         └────────┬────────┘
                                  │
                   ┌──────────────┴──────────────┐
                   │                             │
                   ▼                             ▼
          ┌─────────────────┐           ┌─────────────────┐
          │  Monolith App   │           │ Microservice UI  │
          │  :8080          │           │  (REST Client)   │
          │                 │           └────────┬────────┘
          │ Spring Security │                    │
          │ @PreAuthorize   │                    ▼
          │ ABAC Ownership  │           ┌────────────────┐
          └────────┬────────┘           │  API Gateway   │
                   │                    │  :8090          │
                   │                    │  JWT Validation │
                   │                    │  Header Inject  │
                   └──────────────┐     └───────┬────────┘
                                  │             │
                                  │             ▼
                                  │    ┌──────────────────┐
                                  │    │  Config Server   │
                                  │    └────────┬─────────┘
                                  │             │
                   ┌──────────────┼─────────────┼──────────────┬─────────────┐
                   │              │             │              │             │
                   │              ▼             ▼              ▼             ▼
                   │         User Svc      Product Svc    Order Svc      File Svc
                   │         :8091         :8092          :8093          :8096
                   │                                       │             │
                   │                                       ▼             ▼
                   │                                  Payment Svc      MinIO Storage
                   │                                  :9000            :9000
                   │                                       │
                   │                                       ▼
                   │                                Notification Svc
                   │                                :8095
                   │
                   └──────────────┐
                                  │ OAuth2/OIDC
                                  ▼
                         ┌─────────────────┐
                         │   SSO Server    │
                         │   :9000         │
                         │                 │
                         │ OAuth2          │
                         │ OIDC            │
                         │ JWKS            │
                         │ RBAC            │
                         │ Refresh Token   │
                         │ Audit Log       │
                         └────────┬────────┘
                                  │
                   ┌──────────────┼──────────────┐
                   ▼              ▼              ▼
                Postgres        Redis          Kafka
```
