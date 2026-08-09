# SSO Platform - Lộ Trình Sprint (Sprint Plan Index)

Tài liệu này là chỉ mục tổng hợp lộ trình **25 Sprint** của dự án **SSO Platform**. Sprint plan chi tiết của từng Phase được phân rã thành các file con.

---

## 🗺️ Bản Đồ Lộ Trình Tổng Thể

```
Phase 0 — Foundation & Infrastructure    (Sprint 00-01)
Phase 1 — SSO Server Core               (Sprint 02-05)
Phase 2 — Monolith App                  (Sprint 06-10)
Phase 3 — Microservice App              (Sprint 11-17)
Phase 4 — Security Hardening & Testing  (Sprint 18-21)
Phase 5 — Observability & Production    (Sprint 22-25)
```

---

## Phase 0: Foundation & Infrastructure (Sprint 00-01)

### Sprint 00 — Project Setup & Infrastructure
**Mục tiêu:** Khởi tạo repository, cấu hình toàn bộ infrastructure bằng Docker Compose.

**Tasks:**
- `[ ]` Tạo Maven multi-module project với modules: `sso-server`, `monolith-app`, `microservice-app/*`, `common-contracts`
- `[ ]` Tạo `docker-compose.infra.yml` bao gồm:
  - PostgreSQL 16 (ports: 5432 — shared) với 4 databases: `sso_db`, `monolith_db`, `microservice_db` (cho dev; prod sẽ tách riêng)
  - Redis 7 (:6379)
  - Apache Kafka + Zookeeper (:9092)
  - Kafka UI (:8080)
  - Prometheus (:9090)
  - Grafana (:3000)
- `[ ]` Tạo `.cursorrules`, `clauderules.md`, `geminirules.md` với ngữ cảnh project
- `[ ]` Tạo file `docs/` đầy đủ theo structure
- `[ ]` Setup Flyway migration cho `sso_db` (V1__init_schema.sql)

**Definition of Done:** `docker compose up -d` chạy thành công, tất cả services healthy.

---

### Sprint 01 — Common Library & Database Schema
**Mục tiêu:** Tạo các shared contracts và database schema đầy đủ.

**Tasks:**
- `[ ]` Tạo `common-contracts` module chứa:
  - `ApiResponse<T>` record
  - `ErrorCode` enum
  - `BusinessException` class
  - Kafka event DTOs: `OrderCreatedEvent`, `PaymentCompletedEvent`, `UserRegisteredEvent`
- `[ ]` Viết Flyway migration `sso_db`:
  - `V1__create_users_table.sql`
  - `V2__create_roles_permissions_tables.sql`
  - `V3__create_user_roles_role_permissions.sql`
  - `V4__create_oauth_clients_table.sql`
  - `V5__create_sessions_refresh_tokens_tables.sql`
  - `V6__create_audit_logs_table.sql`
  - `V7__insert_default_roles_permissions.sql` (seed data)
- `[ ]` Viết Flyway migration `monolith_db`:
  - `V1__create_user_profiles_table.sql`
  - `V2__create_products_table.sql`
  - `V3__create_orders_order_items_tables.sql`
  - `V4__create_payments_table.sql`
  - `V5__create_audit_logs_table.sql`
- `[ ]` Viết Flyway migration `order_db` và `product_db` cho Microservice

**Definition of Done:** Chạy Flyway migrate thành công, schema đúng với thiết kế ở Architecture Bible.

---

## Phase 1: SSO Server (Sprint 02-05)

### Sprint 02 — SSO Server Bootstrap & OAuth2 Foundation
**Mục tiêu:** SSO Server khởi động, expose JWKS endpoint, cấu hình OAuth2 clients.

**Tasks:**
- `[ ]` Khởi tạo `sso-server` Spring Boot app với dependencies:
  - `spring-security-oauth2-authorization-server`
  - `spring-boot-starter-security`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-data-redis`
  - `flyway-core`
- `[ ]` Implement `AuthorizationServerConfig.java`:
  - Đăng ký 2 OAuth2 clients: `monolith-web` và `microservice-gateway`
  - Đăng ký 5 service clients (Client Credentials): `order-service`, `payment-service`, v.v.
  - Cấu hình `RegisteredClientRepository` lưu vào PostgreSQL (không dùng InMemory)
  - Token settings: access token TTL = 15 phút, refresh token TTL = 7 ngày
- `[ ]` Implement RSA key pair generation và JWKS endpoint:
  - Tạo `KeyPairConfig.java` generate RSA-2048 key pair
  - Expose `/.well-known/openid-configuration`
  - Expose `/oauth2/jwks`
- `[ ]` Implement `JwtCustomizerConfig.java`:
  - Thêm custom claims vào JWT: `roles`, `permissions`, `email`, `name`
  - Load roles và permissions từ DB của user
- `[ ]` Implement `CustomUserDetailsService.java`:
  - Load user từ PostgreSQL
  - Check `enabled` và `locked` status

**API Contracts:**
```
GET  /.well-known/openid-configuration  → OIDC metadata
GET  /oauth2/jwks                        → Public keys (JWKS JSON)
POST /oauth2/token                       → Exchange code/credentials for token
POST /oauth2/revoke                      → Revoke token
GET  /oauth2/authorize                   → Authorization endpoint
```

**Definition of Done:** Postman call `/oauth2/authorize` → redirect về login page. Sau login → nhận được JWT với custom claims.

---

### Sprint 03 — User & Role Management API
**Mục tiêu:** Hoàn thiện API quản trị User, Role, Permission cho SSO Server.

**Tasks:**
- `[ ]` Implement `UserEntity.java` với các fields:
  ```java
  id (UUID), username, email, passwordHash, firstName, lastName,
  enabled, locked, createdAt, updatedAt, lastLoginAt
  ```
- `[ ]` Implement `RoleEntity.java` và `PermissionEntity.java` với ManyToMany relationship
- `[ ]` Implement `AdminUserController.java`:
  - `POST /admin/users` — Tạo user mới (chỉ ADMIN)
  - `GET /admin/users` — Danh sách users có phân trang
  - `GET /admin/users/{id}` — Chi tiết user
  - `PUT /admin/users/{id}` — Cập nhật thông tin
  - `PUT /admin/users/{id}/status` — Enable/Disable user
  - `POST /admin/users/{id}/roles` — Gán roles
  - `DELETE /admin/users/{id}/roles/{roleId}` — Thu hồi role
- `[ ]` Implement `AdminRoleController.java`:
  - `POST /admin/roles` — Tạo role mới
  - `GET /admin/roles` — Danh sách roles
  - `POST /admin/roles/{id}/permissions` — Gán permissions
- `[ ]` Bảo mật tất cả `/admin/*` endpoints: chỉ ADMIN service account được phép gọi (Client Credentials)
- `[ ]` Viết Unit Test cho UserService và RoleService (Mockito)

**Definition of Done:** CRUD User/Role/Permission API hoạt động, có Javadoc đầy đủ tiếng Việt.

---

### Sprint 04 — Authentication Flow: Login, Logout, Password Change
**Mục tiêu:** Hoàn thiện các luồng xác thực cốt lõi.

**Tasks:**
- `[ ]` Implement `BruteForceProtectionService.java`:
  ```java
  // Dùng Redis: login:attempt:{username} → TTL 5 phút
  // Sau 5 lần thất bại → lock account tạm thời
  // Sau 10 lần → lock vĩnh viễn, cần Admin unlock
  ```
- `[ ]` Implement `AuthController.java`:
  - `POST /auth/change-password` — Đổi mật khẩu (yêu cầu old password)
  - `POST /auth/forgot-password` — Gửi reset link qua email
  - `POST /auth/reset-password?token=...` — Đặt lại mật khẩu
- `[ ]` Implement Refresh Token Rotation:
  - Mỗi lần dùng refresh token → issue refresh token mới, invalidate cũ
  - Nếu refresh token cũ bị dùng lại (replay) → revoke toàn bộ token family (security incident)
- `[ ]` Implement Session Management:
  - Lưu session vào bảng `sessions` (id, user_id, ip, user_agent, created_at)
  - SSO logout → destroy session + revoke tất cả token trong session
- `[ ]` Implement `AuditLogService.java`:
  - Ghi log: `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT`, `PASSWORD_CHANGED`, `TOKEN_REFRESH`
- `[ ]` Implement Rate Limiting cho `/auth/*` endpoints: 10 req/min/IP (Redis)
- `[ ]` Implement Two-Factor Authentication (2FA / TOTP):
  - Setup flow: `/auth/2fa/setup` sinh secret key (Base32) và QR Code URL
  - Verification flow: `/auth/2fa/verify` để kích hoạt
  - Verification during Login: Bắt buộc nhập OTP 6 số nếu user đã enable 2FA
- `[ ]` Implement Social Login (Google / Microsoft OAuth2):
  - Cấu hình OAuth2 login client integration tại SSO Server
  - Account Linking: tự động map email từ Google/Microsoft ID Token sang local account
- `[ ]` Custom giao diện (HTML/CSS templates) cho các trang Login, Consent, và 2FA Verification trên SSO Server để đồng bộ thương hiệu
- `[ ]` Viết Integration Test dùng Testcontainers (PostgreSQL + Redis)

**Definition of Done:** Login → lấy được token. Đăng nhập sai 5 lần → bị lock. Đổi refresh token → token cũ bị invalidate. Xác thực 2FA hoạt động chính xác. Giao diện Login/2FA custom hiển thị đồng bộ.

---

### Sprint 05 — SSO Cross-App Session (Single Sign-On)
**Mục tiêu:** Chứng minh SSO thực sự hoạt động giữa Monolith và Microservice App.

**Tasks:**
- `[ ]` Cấu hình SSO Server session persistence với Redis (không dùng in-memory)
  - Lý do: Trong môi trường nhiều SSO Server instances, session phải shared
- `[ ]` Test SSO flow đầy đủ:
  1. Login vào Monolith App → SSO redirect về Monolith
  2. Mở Microservice App → SSO phát hiện session đã tồn tại → không hỏi password lại
  3. Logout khỏi một app → Cả 2 app bị logout
- `[ ]` Implement Back-Channel Logout (SSO notify apps khi user logout):
  - SSO gọi `POST {app}/logout` của từng registered client
- `[ ]` Document toàn bộ flow bằng Sequence Diagram trong `docs/sequences/`
- `[ ]` Viết Security Test: JWT tampering, expired token, wrong issuer

**Definition of Done:** SSO hoạt động giữa 2 app. Logout từ app A → app B cũng bị logout.

---

## Phase 2: Monolith App (Sprint 06-10)

### Sprint 06 — Monolith Bootstrap & OAuth2 Client Integration
**Mục tiêu:** Monolith App khởi động, tích hợp SSO login, nhận được JWT.

**Tasks:**
- `[ ]` Khởi tạo `monolith-app` Spring Boot với dependencies:
  - `spring-boot-starter-security`
  - `spring-boot-starter-oauth2-client` (OAuth2 Login)
  - `spring-boot-starter-oauth2-resource-server` (JWT validation)
  - `spring-boot-starter-data-jpa`
- `[ ]` Implement `SecurityConfig.java`:
  ```java
  @Configuration
  @EnableWebSecurity
  @EnableMethodSecurity  // Bật @PreAuthorize, @PostAuthorize
  public class SecurityConfig {
      // oauth2Login → redirect đến SSO
      // oauth2ResourceServer → validate JWT từ JWKS của SSO
  }
  ```
- `[ ]` Implement `SsoJwtGrantedAuthoritiesConverter.java`:
  - Đọc `roles` và `permissions` từ JWT claims
  - Convert thành `SimpleGrantedAuthority` để `@PreAuthorize` hoạt động
- `[ ]` Implement `UserProfileEntity.java` (chứa extended profile info, FK = SSO user UUID)
- `[ ]` Implement `UserProfileController.java`:
  - `GET /api/users/me` — Lấy profile của chính mình
  - `PUT /api/users/me` — Cập nhật profile
- `[ ]` Cấu hình CORS cho `http://localhost:3000` (Frontend dev server)
- `[ ]` Khởi tạo dự án Next.js 15 Frontend (`sso-platform-ui`) sử dụng TypeScript và CSS Modules
- `[ ]` Cấu hình **NextAuth.js v5** kết nối đăng nhập OAuth2 OIDC với SSO Server (:9000)
- `[ ]` Thiết kế khung giao diện Dashboard Shell (Sidebar, Header, DashboardShell) có responsive
- `[ ]` Xây dựng trang Landing/Login và Route Callback handler để nhận token từ SSO Server

**Definition of Done:** `GET /api/users/me` trả về profile thành công. Dự án Next.js khởi tạo chạy được, có thể đăng nhập/đăng xuất qua SSO Server và hiển thị trang Dashboard Shell trống.

---

### Sprint 07 — Monolith Product & @PreAuthorize
**Mục tiêu:** CRUD Product với phân quyền `@PreAuthorize` đầy đủ.

**Tasks:**
- `[ ]` Implement `ProductEntity.java`, `ProductRepository.java`
- `[ ]` Implement `ProductService.java` với `@PreAuthorize` **ở Service Layer**:
  ```java
  @PreAuthorize("hasAuthority('PRODUCT_READ')")
  public Page<ProductResponse> getProducts(Pageable pageable) { ... }

  @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
  public ProductResponse createProduct(CreateProductRequest req) { ... }

  @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
  public ProductResponse updateProduct(Long id, UpdateProductRequest req) { ... }

  // Chỉ ADMIN hoặc MANAGER mới được xóa sản phẩm
  @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
  public void deleteProduct(Long id) { ... }
  ```
- `[ ]` Implement `ProductController.java` (không có `@PreAuthorize` ở Controller)
- `[ ]` Viết Unit Test cho từng security scenario:
  - USER gọi `createProduct` → `AccessDeniedException`
  - STAFF gọi `createProduct` → thành công
  - USER gọi `deleteProduct` → `AccessDeniedException`
  - ADMIN gọi `deleteProduct` → thành công
- `[ ]` Xây dựng trang Danh sách sản phẩm (Product List) và Chi tiết sản phẩm (Product Detail) hiển thị dữ liệu sử dụng React Server Components (RSC)
- `[ ]` Triển khai client component `ProductForm` phục vụ việc Thêm/Sửa sản phẩm
- `[ ]` Sử dụng Custom Hook `usePermission` để thực hiện conditional rendering ẩn/hiện nút Thêm/Sửa/Xóa tùy theo Roles & Permissions của user hiện tại

**Definition of Done:** Tất cả unit test backend pass. Giao diện xem danh sách và chi tiết sản phẩm hoạt động. Giao diện CRUD sản phẩm phân quyền chính xác theo permissions của user đăng nhập.

---

### Sprint 08 — Monolith Order Service & Resource Ownership
**Mục tiêu:** Implement ABAC (Resource Ownership) — user chỉ xem/hủy đơn hàng của chính mình.

**Tasks:**
- `[ ]` Implement `OrderEntity.java`, `OrderItemEntity.java`, `OrderRepository.java`
- `[ ]` Implement `OrderSecurityEvaluator.java` (Spring Bean cho SpEL):
  ```java
  /**
   * Security evaluator để kiểm tra ownership của đơn hàng.
   *
   * Tại sao cần class riêng thay vì check trong Service?
   * - Cho phép dùng cú pháp SpEL trong @PreAuthorize:
   *   @PreAuthorize("@orderSecurity.isOwnerOrAdmin(authentication, #orderId)")
   * - Separation of Concerns: security logic tách khỏi business logic
   * - Dễ mock trong unit test
   */
  @Component("orderSecurity")
  public class OrderSecurityEvaluator {

      public boolean isOwnerOrAdmin(Authentication auth, Long orderId) {
          // Check nếu user là ADMIN → return true
          // Nếu không, check order.userId == currentUser.id
      }
  }
  ```
- `[ ]` Implement `OrderService.java`:
  ```java
  @PreAuthorize("hasAuthority('ORDER_CREATE')")
  public OrderResponse createOrder(CreateOrderRequest req) { ... }

  // ADMIN thấy tất cả orders, USER chỉ thấy của mình
  @PreAuthorize("hasAuthority('ORDER_READ') and @orderSecurity.isOwnerOrAdmin(authentication, #orderId)")
  public OrderResponse getOrder(Long orderId) { ... }

  // Chỉ owner hoặc ADMIN mới được hủy
  @PreAuthorize("@orderSecurity.isOwnerOrAdmin(authentication, #orderId)")
  public void cancelOrder(Long orderId) { ... }
  ```
- `[ ]` Implement `PaymentService.java` (Mock Sandbox):
  ```java
  @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
  public PaymentResponse processPayment(ProcessPaymentRequest req) { ... }
  ```
- `[ ]` Viết Integration Test: test ownership violation → HTTP 403
- `[ ]` Xây dựng giao diện Danh sách đơn hàng (Order List) và Chi tiết đơn hàng (Order Detail) hỗ trợ phân quyền sở hữu tài nguyên (Ownership check)
- `[ ]` Xây dựng giao diện Checkout đơn hàng (chọn sản phẩm, nhập địa chỉ) và tích hợp trang thanh toán giả lập (Mock Sandbox Payment Page)

**Definition of Done:** USER không thể xem đơn hàng của người khác. ADMIN có thể xem tất cả. Integration test pass. Giao diện Đơn hàng và Checkout hoạt động đúng phân quyền.

---

### Sprint 09 — Monolith Audit Log & Security Hardening
**Mục tiêu:** Audit log tự động, bảo mật nâng cao cho Monolith.

**Tasks:**
- `[ ]` Implement `@Auditable` annotation:
  ```java
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Auditable {
      String action();       // Ví dụ: "ORDER_CREATED"
      String resource();     // Ví dụ: "Order"
  }
  ```
- `[ ]` Implement `AuditLogAspect.java` (Spring AOP):
  - Intercept `@Auditable` methods
  - Ghi audit log: `actor_id`, `action`, `resource`, `resource_id`, `ip`, `timestamp`
  - Log cả thành công lẫn thất bại (trong `@AfterThrowing` advice)
- `[ ]` Security Headers cấu hình tại `SecurityConfig`:
  - `X-Frame-Options: DENY` (chống Clickjacking)
  - `X-Content-Type-Options: nosniff` (chống MIME sniffing)
  - `Content-Security-Policy: default-src 'self'`
  - `Strict-Transport-Security: max-age=31536000` (HSTS)
- `[ ]` Implement CSRF protection cho non-API endpoints (nếu có Thymeleaf views)
- `[ ]` Viết Security Attack Test:
  - Test JWT với `alg:none` → phải bị reject
  - Test JWT với wrong issuer → HTTP 401
  - Test JWT với wrong audience → HTTP 401
  - Test tampered JWT payload → HTTP 401
- `[ ]` Xây dựng trang Quản trị Admin: Giao diện xem danh sách User, thay đổi trạng thái Active/Locked, và gán/thu hồi Roles
- `[ ]` Xây dựng giao diện hiển thị Centralized Audit Logs lọc theo User, Action (chỉ cho phép ADMIN / AUDITOR truy cập)

**Definition of Done:** Tất cả security tests pass. Audit logs được ghi đầy đủ. Trang quản lý User và xem Audit Log trên Frontend hoạt động đúng phân quyền.

---

### Sprint 10 — Monolith Complete Integration Test
**Mục tiêu:** Integration test end-to-end cho toàn bộ Monolith.

**Tasks:**
- `[ ]` Setup Testcontainers cho PostgreSQL và Redis
- `[ ]` Viết Integration Test scenarios:
  - Scenario 1 — Happy path: LOGIN → GET /products → POST /orders → GET /orders/{id}
  - Scenario 2 — Auth failure: Expired JWT → 401
  - Scenario 3 — Authorization failure: USER → DELETE /products → 403
  - Scenario 4 — Ownership violation: USER A → GET /orders/{orderId of User B} → 403
  - Scenario 5 — Brute force: 6 login attempts → account locked → 429
- `[ ]` Load test nhỏ với k6: 100 concurrent users, 60 seconds → P99 < 500ms
- `[ ]` Cài đặt và cấu hình thư viện kiểm thử giao diện E2E (Playwright hoặc Cypress) cho Next.js App
- `[ ]` Viết luồng kiểm thử giao diện tự động (E2E Test) bao phủ luồng đi đầy đủ (Happy path E2E): Login -> Mua hàng -> Checkout -> Xem đơn hàng -> Đổi mật khẩu -> Logout

**Definition of Done:** Tất cả Integration Test backend pass. E2E Test trên UI chạy thành công hoàn toàn không có lỗi hiển thị/chức năng. Monolith app & UI production-ready.

---

## Phase 3: Microservice App (Sprint 11-17)

### Sprint 11 — API Gateway Bootstrap & JWT Validation
**Mục tiêu:** API Gateway xác thực JWT từ JWKS của SSO Server, inject trusted headers.

**Tasks:**
- `[ ]` Khởi tạo `api-gateway` Spring Cloud Gateway app
- `[ ]` Implement `StripClientHeadersFilter.java`:
  ```java
  /**
   * Filter QUAN TRỌNG: Strip các X-User-* headers do client gửi lên.
   * Tại sao phải strip trước khi validate JWT?
   * - Nếu không, attacker gửi header X-User-Id: 1 (admin) sẽ bypass authorization
   * - Gateway là Trust Boundary duy nhất giữa Internet và Internal Network
   * - Chỉ sau khi validate JWT, Gateway mới inject trusted headers
   */
  public class StripClientHeadersFilter implements GlobalFilter { ... }
  ```
- `[ ]` Implement `JwtValidationFilter.java`:
  - Fetch JWKS từ `http://sso-server:9000/oauth2/jwks`
  - Cache JWKS keys (refresh khi nhận `kid` không biết)
  - Validate: signature, expiration, issuer (`iss`), audience (`aud`)
  - Trả về 401 với error JSON nếu invalid
- `[ ]` Implement `TrustedHeaderInjectionFilter.java`:
  - Extract `sub` → `X-User-Id`
  - Extract `roles` → `X-User-Roles`
  - Extract `permissions` → `X-User-Permissions`
  - Extract `email` → `X-User-Email`
- `[ ]` Cấu hình routes trong `application.yml` đi qua Eureka:
  ```yaml
  /api/users/**     → user-service:8091
  /api/products/**  → product-service:8092
  /api/orders/**    → order-service:8093
  /api/payments/**  → payment-service:8094
  /api/files/**     → file-service:8096
  ```
- `[ ]` Implement Rate Limiting: 100 req/min/user (Redis Token Bucket)
- `[ ]` Cấu hình môi trường Frontend chuyển hướng endpoint API từ Monolith sang API Gateway (:8090)
- `[ ]` Cập nhật Base Fetch client để tự động đính kèm Access Token trong authorization header khi gọi các API qua Gateway

**Definition of Done:** Request với valid JWT → Gateway inject headers → forward đến service. Invalid JWT → 401. Fake X-User headers từ client → bị strip. Giao diện người dùng Next.js gọi thành công API qua cổng Gateway.

---

### Sprint 12 — Centralized Config, Service Discovery & Microservice Skeleton
**Mục tiêu:** Eureka Server + Config Server + skeleton cho 6 microservices, mỗi service đọc X-User headers.

**Tasks:**
- `[ ]` Khởi tạo Spring Cloud Config Server (`:8888`) quản lý cấu hình tập trung
- `[ ]` Khởi tạo Eureka Server (`:8761`)
- `[ ]` Tạo skeleton cho 6 services (user, product, order, payment, notification, file-service):
  - `CurrentUserResolver.java`:
    ```java
    /**
     * Đọc thông tin user đã được Gateway xác thực và inject vào headers.
     * Tại sao không parse JWT trực tiếp?
     * - Gateway đã validate JWT và chịu trách nhiệm authentication
     * - Services chỉ cần đọc trusted headers (tiết kiệm CPU, giảm latency)
     * - Headers đến từ Internal Network sau Trust Boundary → an toàn
     */
    @Component
    public class CurrentUserResolver {
        public CurrentUser resolve(HttpServletRequest request) {
            String userId = request.getHeader("X-User-Id");
            String roles = request.getHeader("X-User-Roles");
            String permissions = request.getHeader("X-User-Permissions");
            // parse và tạo CurrentUser record
        }
    }
    ```
  - `AuthorizationService.java` (dùng chung pattern)
  - `GlobalExceptionHandler.java`
  - `application.yml` (Eureka & Config Server registration, port mapping)
- `[ ]` Kiểm tra service discovery: tất cả 6 services hiện trên Eureka dashboard

**Definition of Done:** Tất cả services khởi động thành công, nhận config từ Config Server và đăng ký với Eureka.

---

### Sprint 13 — User Service & Product Service
**Mục tiêu:** Implement User Profile Service và Product Service với authorization đầy đủ.

**Tasks:**

**User Service (:8091)**
- `[ ]` `GET /api/users/me` — Trả về profile của current user
- `[ ]` `PUT /api/users/me` — Cập nhật profile (chỉ của chính mình)
- `[ ]` `GET /api/users/{id}` — ADMIN/SUPPORT only
- `[ ]` `PUT /api/users/{id}/status` — Enable/Disable, ADMIN only

**Product Service (:8092)**
- `[ ]` `GET /api/products` — Public (không cần auth)
- `[ ]` `GET /api/products/{id}` — Public
- `[ ]` `POST /api/products` — `requirePermission(PRODUCT_CREATE)`
- `[ ]` `PUT /api/products/{id}` — `requirePermission(PRODUCT_UPDATE)`
- `[ ]` `DELETE /api/products/{id}` — `requireRole(ADMIN)` hoặc `requireRole(MANAGER)`

**File Service (:8096) & MinIO Integration**
- `[ ]` Khởi động MinIO service ở docker-compose.infra.yml
- `[ ]` Implement API upload file: `POST /api/files/upload` (validate MIME type, size < 5MB)
- `[ ]` Cấu hình bucket policies và sinh Presigned URLs cho ảnh private
- `[ ]` Cung cấp public URLs cho avatars và product images

**Authorization pattern:**
```java
public ProductResponse createProduct(CreateProductRequest req, CurrentUser currentUser) {
    // Tường minh hơn @PreAuthorize — dev đọc code biết ngay cần quyền gì
    authorizationService.requirePermission(currentUser, "PRODUCT_CREATE");
    // ... business logic
}
```

- `[ ]` Xây dựng trang cá nhân (Profile Page) hiển thị thông tin và hỗ trợ chỉnh sửa thông tin người dùng
- `[ ]` Triển khai API tích hợp tải ảnh lên (upload avatar) từ Frontend qua `file-service` lưu vào MinIO, tự động hiển thị ảnh đại diện mới trên Header
- `[ ]` Thiết kế Modal Setup 2FA: Tạo nút Switch bật 2FA, hiển thị mã QR và trường nhập mã OTP 6 số để kích hoạt 2FA

**Definition of Done:** API hoạt động đúng. USER gọi PRODUCT_CREATE → 403. STAFF gọi → 201. File Upload lên MinIO thành công, trả về URL truy cập được. Giao diện Profile hoạt động, có thể upload avatar và bật 2FA thông qua QR code thành công.

---

### Sprint 14 — Order Service & Resource Ownership
**Mục tiêu:** Order Service với ownership check và Idempotency Key.

**Tasks:**
- `[ ]` `POST /api/orders` với Idempotency-Key header:
  ```java
  /**
   * Idempotency key chống duplicate order khi user click 2 lần.
   * Nếu cùng key đã xử lý thành công → trả về cached result, không tạo order mới.
   * Lưu key vào Redis với TTL 24 giờ.
   */
  public OrderResponse createOrder(CreateOrderRequest req, String idempotencyKey, CurrentUser user) {
      authorizationService.requirePermission(user, "ORDER_CREATE");
      // Check idempotency key trong Redis
      // Nếu đã có → trả về cached response
      // Nếu chưa → tạo order mới, cache response
  }
  ```
- `[ ]` `GET /api/orders/{id}` với ownership check:
  ```java
  public OrderResponse getOrder(Long orderId, CurrentUser currentUser) {
      Order order = orderRepository.findById(orderId).orElseThrow(...);
      // Resource ownership check
      authorizationService.requireOwnerOrAdmin(currentUser, order.getUserId());
      return mapToResponse(order);
  }
  ```
- `[ ]` `GET /api/orders` — USER thấy đơn của mình, ADMIN thấy tất cả
- `[ ]` `POST /api/orders/{id}/cancel` — Chỉ owner hoặc ADMIN, chỉ khi status = PENDING
- `[ ]` Nâng cấp giao diện Checkout để tự động tạo UUID `Idempotency-Key` gửi lên header, tránh việc bấm nút đặt hàng nhiều lần dẫn đến trùng lặp đơn

**Definition of Done:** Ownership test: User A không thể xem order của User B → 403. Giao diện Checkout gửi kèm Idempotency-Key và hiển thị chính xác kết quả đặt hàng, click 2 lần liên tiếp không bị trùng đơn hàng.

---

### Sprint 15 — Payment Service & Service-to-Service Auth
**Mục tiêu:** Payment Service với Client Credentials authentication từ Order Service.

**Tasks:**
- `[ ]` Implement `OrderService → PaymentService` call với OAuth2 Client Credentials:
  ```java
  /**
   * Gọi Payment Service bằng Service Access Token (không phải User Token).
   * Tại sao cần token riêng cho service-to-service?
   * - User token có scope giới hạn cho user đó
   * - Service token đại diện cho Order Service là client tin cậy
   * - Payment Service cần phân biệt: request từ human user hay từ internal service
   */
  @Service
  public class PaymentServiceClient {
      private final OAuth2AuthorizedClientManager clientManager; // quản lý service token

      public PaymentResponse requestPayment(Long orderId, BigDecimal amount) {
          // Tự động fetch service token nếu hết hạn
          OAuth2AuthorizedClient client = clientManager.authorize(...);
          String serviceToken = client.getAccessToken().getTokenValue();
          // Gọi Payment Service với Bearer serviceToken
      }
  }
  ```
- `[ ]` Payment Service chỉ cho phép request từ `order-service` client:
  ```java
  // Validate client_id trong token == "order-service"
  ```
- `[ ]` Implement Mock Payment: PENDING → COMPLETED (sau 2 giây mock)
- `[ ]` Implement `POST /api/payments/{id}/refund` — MANAGER hoặc ADMIN only
- `[ ]` Xây dựng giao diện xem trạng thái hoạt động các microservices (Eureka Dashboard / Instances health check page) dành riêng cho ADMIN

**Definition of Done:** Order Service gọi Payment Service thành công với service token. Direct call từ bên ngoài (user token) → 403. Giao diện quản trị Admin hiển thị đúng danh sách các service đang UP/DOWN từ Eureka API.

---

### Sprint 16 — Kafka Event-Driven & Outbox Pattern
**Mục tiêu:** Kết nối các services qua Kafka, implement Outbox Pattern.

**Tasks:**
- `[ ]` Tạo Kafka topics:
  - `order-created` (3 partitions)
  - `payment-completed` (3 partitions)
  - `payment-failed` (3 partitions)
  - `order-status-changed` (3 partitions)
- `[ ]` Implement Transactional Outbox trong Order Service:
  ```java
  /**
   * Outbox Pattern: ghi event vào DB cùng transaction với order.
   * Tại sao không publish Kafka trực tiếp trong @Transactional?
   * - Nếu Kafka down sau khi DB commit → event bị mất vĩnh viễn
   * - Outbox đảm bảo: DB commit ↔ event đều thành công hoặc đều rollback
   */
  @Transactional
  public OrderResponse createOrder(CreateOrderRequest req, CurrentUser user) {
      Order order = orderRepository.save(...);
      // Ghi event vào bảng outbox_events CÙNG transaction
      outboxRepository.save(OutboxEvent.builder()
          .eventType("ORDER_CREATED")
          .payload(objectMapper.writeValueAsString(new OrderCreatedEvent(...)))
          .build());
      return mapToResponse(order);
  }
  ```
- `[ ]` Implement `OutboxEventPublisher.java` (Scheduled Job):
  - Chạy mỗi 5 giây, đọc outbox events chưa gửi, publish lên Kafka
- `[ ]` Implement `NotificationService` consume `order-created`:
  - Log "Gửi email xác nhận đơn hàng" (mock gửi email)
  - Implement Idempotent Consumer (check event_id đã xử lý chưa)
- `[ ]` Xây dựng giao diện Centralized Reports Dashboard hiển thị các biểu đồ trực quan về doanh thu bán hàng, số lượng đơn hàng, và biểu đồ tài nguyên (CPU, Memory, Request Rate)

**Definition of Done:** Tạo order → outbox event → Kafka → Notification Service log email confirmation. Giao diện báo cáo và biểu đồ metrics hiển thị dữ liệu chính xác trên Next.js Dashboard.

---

### Sprint 17 — Resilience4j & Circuit Breaker
**Mục tiêu:** Order Service không bị sập khi Payment Service down.

**Tasks:**
- `[ ]` Cài Resilience4j cho `PaymentServiceClient`:
  - Circuit Breaker: 50% lỗi trong 10s → OPEN, 30s sau thử HALF-OPEN
  - Retry: 3 lần, delay 500ms, exponential backoff
  - Timeout: 3 giây
  - Bulkhead: tối đa 10 concurrent calls tới Payment Service
- `[ ]` Implement Fallback:
  ```java
  @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
  @Retry(name = "paymentService")
  @TimeLimiter(name = "paymentService")
  public CompletableFuture<PaymentResponse> requestPayment(...) { ... }

  private CompletableFuture<PaymentResponse> paymentFallback(Throwable ex) {
      // Đặt order vào trạng thái PAYMENT_PENDING
      // Sẽ retry sau khi Payment Service phục hồi
      log.warn("Payment Service unavailable, order queued for retry");
      return CompletableFuture.completedFuture(PaymentResponse.queued());
  }
  ```
- `[ ]` Test: Dừng Payment Service → Order Service trả về fallback response → Circuit Breaker OPEN
- `[ ]` Khởi động lại Payment Service → Circuit Breaker chuyển HALF-OPEN → CLOSED
- `[ ]` Xây dựng Fallback UI / Error Boundary thân thiện khi API Gateway báo lỗi timeout hoặc 503 Service Unavailable (khi Circuit Breaker ở trạng thái OPEN), hiển thị thông báo "Hệ thống đang bận, xin vui lòng thử lại sau"

**Definition of Done:** Order Service không bị sập khi Payment Service down. Circuit Breaker hoạt động đúng states. Giao diện Frontend hiển thị Fallback UI khi backend bị sập hoặc gặp sự cố quá tải.

---

## Phase 4: Security Hardening & Testing (Sprint 18-21)

### Sprint 18 — Security Attack Scenarios Testing
**Mục tiêu:** Tự tấn công hệ thống để verify tất cả security controls hoạt động.

**Tasks:**
- `[ ]` JWT Attack Tests:
  - `alg:none` attack → Gateway reject
  - Expired JWT → 401
  - Wrong issuer (`iss`) → 401
  - Wrong audience (`aud`) → 401
  - Tampered payload (thay `roles: ["USER"]` → `["ADMIN"]`) → Invalid signature → 401
  - Replay attack (dùng lại access token đã revoke) → 401
- `[ ]` Header Spoofing Test:
  - Client tự gửi `X-User-Id: 1` (admin) → Gateway strip → 403
  - Client tự gửi `X-User-Roles: ADMIN` → Strip → forbidden
- `[ ]` Privilege Escalation Test:
  - USER cố gọi `DELETE /api/products/1` → 403
  - USER cố gọi `GET /api/orders/{orderId của người khác}` → 403
- `[ ]` IDOR Test (Insecure Direct Object Reference):
  - Bruteforce orderId: `GET /api/orders/1`, `/orders/2`... → Phải 403 nếu không phải của mình
- `[ ]` Refresh Token Replay Test:
  - Dùng RT → nhận RT mới → Dùng RT cũ lần nữa → System detect replay → Revoke cả family
- `[ ]` Brute Force Test:
  - Script 6 lần login sai liên tiếp → Account locked → 429
- `[ ]` Giao diện E2E Security Tests: Kiểm tra xử lý tự động khi Token hết hạn (NextAuth.js token rotation & auto-redirect), bảo mật XSS chống script injection trên input forms, và kiểm tra cookie bảo mật (HttpOnly, SameSite)

**Definition of Done:** Tất cả attack scenarios bị block đúng cách. Giao diện người dùng xử lý bảo mật chính xác, tự động logout hoặc refresh token khi hết hạn mà không gây lỗi giao diện.

---

### Sprint 19 — Distributed Tracing & Correlation ID
**Mục tiêu:** Trace được đường đi của request qua tất cả microservices.

**Tasks:**
- `[ ]` Setup OpenTelemetry Agent cho tất cả microservices
- `[ ]` Cấu hình Jaeger hoặc Zipkin collector
- `[ ]` Gateway generate `X-Correlation-Id` cho mỗi request
- `[ ]` Tất cả services log với `correlationId` trong MDC:
  ```java
  MDC.put("correlationId", correlationId);
  log.info("Processing order creation for user: {}", currentUser.userId());
  ```
- `[ ]` Test: Tạo order → Check Jaeger UI → thấy trace: Gateway → Order Service → Payment Service

**Definition of Done:** Tracing hoạt động. Có thể tìm toàn bộ log của một request bằng correlationId.

---

### Sprint 20 — Key Rotation
**Mục tiêu:** Implement RSA key rotation an toàn mà không làm mất tính hợp lệ của tokens hiện có.

**Tasks:**
- `[ ]` Implement Key Rotation API (ADMIN only):
  - `POST /admin/keys/rotate` — Generate key pair mới (key-v2)
  - SSO tiếp tục publish cả key-v1 và key-v2 qua JWKS
  - Tokens cũ signed by key-v1 vẫn valid
  - Tokens mới signed by key-v2
- `[ ]` Sau TTL của access token (15 phút), key-v1 có thể remove khỏi JWKS
- `[ ]` Test: Rotate key → Cũ tokens vẫn valid → Sau 15 phút, key-v1 removed → Cũ tokens expire naturally

**Definition of Done:** Key rotation không làm gián đoạn service. Tokens transition smooth.

---

### Sprint 21 — Complete Security Test Suite
**Mục tiêu:** Viết full automated security test suite.

**Tasks:**
- `[ ]` Setup Testcontainers test environment: SSO + Gateway + User Service + Order Service
- `[ ]` Viết SecurityTestSuite với các scenarios từ Sprint 18 (automated)
- `[ ]` Viết E2E Test: Full user journey từ login đến checkout
- `[ ]` Implement Test Report HTML
- `[ ]` Load test với k6: 500 concurrent users, 5 phút → Error rate < 1%, P99 < 1s

**Definition of Done:** Automated security test suite chạy pass. Load test pass.

---

## Phase 5: Observability & Production (Sprint 22-25)

### Sprint 22 — Prometheus & Grafana Dashboards
**Mục tiêu:** Metrics cho tất cả services hiển thị trên Grafana.

**Tasks:**
- `[ ]` Configure `spring-boot-starter-actuator` + Micrometer cho mọi service
- `[ ]` Prometheus scrape từ `/actuator/prometheus` của tất cả services
- `[ ]` Tạo Grafana dashboards:
  - **Dashboard 1 — System Overview:** HTTP request rate, error rate, latency P50/P95/P99
  - **Dashboard 2 — JVM Health:** Heap memory, GC pauses, thread count, CPU
  - **Dashboard 3 — Business Metrics:** Orders/minute, Payment success rate, Active sessions
  - **Dashboard 4 — Security:** Failed logins/minute, Blocked IPs, Circuit Breaker states

**Definition of Done:** Grafana hiển thị realtime metrics. Có thể thấy spike khi load test.

---

### Sprint 23 — Structured Logging & Log Aggregation
**Mục tiêu:** Tất cả services log theo format JSON, có thể search theo correlationId.

**Tasks:**
- `[ ]` Cấu hình Logback JSON format cho tất cả services:
  ```json
  {
    "timestamp": "2026-08-09T10:00:00Z",
    "level": "INFO",
    "service": "order-service",
    "correlationId": "req-8a92",
    "userId": "user-uuid-123",
    "message": "Order created successfully",
    "orderId": 456
  }
  ```
- `[ ]` Setup Loki + Promtail để thu thập logs từ Docker containers
- `[ ]` Tạo Grafana Loki panel để search logs theo `correlationId`, `userId`, `service`

**Definition of Done:** Có thể search "tất cả logs của request X" trong Grafana Loki.

---

### Sprint 24 — Docker Production Build, GitLab CI & Kubernetes
**Mục tiêu:** Đóng gói toàn bộ hệ thống vào Docker images, chạy qua docker compose, cấu hình GitLab CI pipeline và deploy lên Kubernetes.

**Tasks:**
- `[ ]` Viết multi-stage `Dockerfile` cho mỗi service:
  - Stage 1: Maven build (JDK 21 image)
  - Stage 2: Runtime (JRE 21 slim image, ~150MB)
- `[ ]` Tạo `docker-compose.full.yml`:
  - Tất cả infra (bao gồm MinIO) + tất cả services + Config Server
  - Health checks cho mỗi service
  - Networks: `external-net` (Gateway) và `internal-net` (service-to-service)
  - Volume mounting cho PostgreSQL & MinIO data persistence
- `[ ]` Cấu hình environment variables cho production secrets (không hardcode)
- `[ ]` Viết `.gitlab-ci.yml` định nghĩa CI/CD pipeline với stages: build, test, docker-build, deploy
- `[ ]` Viết các file Kubernetes manifests (Deployment, Service, Ingress, ConfigMap, Secret) cho cụm local/staging
- `[ ]` Cấu hình Nginx Ingress Controller làm reverse proxy thay cho Gateway ở môi trường K8s
- `[ ]` Viết multi-stage `Dockerfile` cho Next.js Frontend (chạy standalone Node.js server) và tích hợp vào `docker-compose.full.yml` cũng như K8s deployment manifests
- `[ ]` Test cold start: `docker compose up --build` từ đầu → toàn bộ hệ thống healthy trong 120s

**Definition of Done:** `docker compose up --build` → mọi thứ chạy (bao gồm cả Next.js Frontend). Health endpoints trả về UP. CI pipeline pass hoàn toàn. K8s manifests sẵn sàng cho cả backend & frontend.

---

### Sprint 25 — Documentation & Interview Preparation
**Mục tiêu:** Hoàn thiện tài liệu, interview guide, ADR cho toàn bộ dự án.

**Tasks:**
- `[ ]` Viết ADR (Architecture Decision Records) cho các quyết định quan trọng:
  - `ADR-001: Tại sao dùng Spring Authorization Server thay vì Keycloak`
  - `ADR-002: Tại sao Gateway validate JWT thay vì gọi Auth Service`
  - `ADR-003: Tại sao dùng Asymmetric Key thay vì Shared Secret`
  - `ADR-004: Tại sao dùng @PreAuthorize ở Monolith nhưng AuthorizationService ở Microservice`
  - `ADR-005: Tại sao dùng Outbox Pattern thay vì publish Kafka trực tiếp`
  - `ADR-006: Tại sao Refresh Token Rotation và cách detect replay attack`
- `[ ]` Tạo Interview Question Bank (200+ câu) về SSO/Security/Distributed System
- `[ ]` Viết `README.md` đẹp với system diagram, feature list, tech stack
- `[ ]` Viết `docs/RUN_GUIDE.md` hướng dẫn chạy project từ đầu
- `[ ]` Viết `docs/SECURITY_MODEL.md` giải thích đầy đủ security model

**Definition of Done:** Tài liệu đầy đủ. Project showcase-ready.

---

## Tóm Tắt Lộ Trình

| Phase | Sprints | Mục Tiêu Chính |
|---|---|---|
| Phase 0 | 00-01 | Infrastructure, DB Schema, Common Library |
| Phase 1 | 02-05 | SSO Server: OAuth2, OIDC, JWT, RBAC |
| Phase 2 | 06-10 | Monolith App: @PreAuthorize, ABAC, Audit |
| Phase 3 | 11-17 | Microservice: Gateway, Services, Kafka, Resilience |
| Phase 4 | 18-21 | Security Testing, Key Rotation, Tracing |
| Phase 5 | 22-25 | Observability, Docker, Documentation |
