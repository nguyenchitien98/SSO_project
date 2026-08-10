# SSO Platform - Lộ Trình Sprint (Sprint Plan Index)

Tài liệu này là chỉ mục tổng hợp lộ trình **25 Sprint** của dự án **SSO Platform**. Sprint plan chi tiết của từng Phase được phân rã thành các file con.

---

## 🗺️ Bản Đồ Lộ Trình Tổng Thể

```
Phase 0   — Foundation & Infrastructure          (Sprint 00-01)
Phase 1   — SSO Server Core                      (Sprint 02-05)
Phase 2   — Monolith App (Backend)               (Sprint 06-10)
Phase 2.5 — Monolith Frontend (React.js + Vite)  (Sprint 10.5)  ← MỚI
Phase 2.6 — Microservice Frontend (Next.js 15)   (Sprint 10.6)  ← MỚI
Phase 3   — Microservice App (Backend)           (Sprint 11-17)
Phase 4   — Security Hardening & Testing         (Sprint 18-21)
Phase 5   — Observability & Production           (Sprint 22-25)
```

---

## Phase 0: Foundation & Infrastructure (Sprint 00-01)

### Sprint 00 — Project Setup & Infrastructure
**Mục tiêu:** Khởi tạo repository, cấu hình toàn bộ infrastructure bằng Docker Compose.

**Tasks:**
- `[x] Tạo Maven multi-module project với modules: sso-server, monolith-app, microservice-app/*, common-contracts`
- `[x] Tạo docker-compose.infra.yml bao gồm:`
  - PostgreSQL 16 (ports: 5432 — shared) với các databases cần thiết
  - Redis 7 (:6379)
  - Apache Kafka + Zookeeper (:9092)
  - Kafka UI (:8081)
  - Prometheus (:9090)
  - Loki & Promtail & Grafana (:3001)
- `[x] Tạo .cursorrules, clauderules.md, geminirules.md với ngữ cảnh project`
- `[x] Tạo file docs/ đầy đủ theo structure`
- `[x] Setup Flyway migration cho sso_db (V1__init_schema.sql)`

**Definition of Done:** `docker compose up -d` chạy thành công, tất cả services healthy.

---

### Sprint 01 — Common Library & Database Schema
**Mục tiêu:** Tạo các shared contracts và database schema đầy đủ.

**Tasks:**
- `[x] Tạo common-contracts module chứa:`
  - `ApiResponse<T>` record
  - `ErrorCode` enum
  - `BusinessException` class
  - Kafka event DTOs: `OrderCreatedEvent`, `PaymentCompletedEvent`, `UserRegisteredEvent`
- `[x] Viết Flyway migration sso_db:`
  - `V1__create_users_table.sql`
  - `V2__create_roles_permissions_tables.sql`
  - `V3__create_user_roles_role_permissions.sql`
  - `V4__create_oauth_clients_table.sql`
  - `V5__create_sessions_refresh_tokens_tables.sql`
  - `V6__create_audit_logs_table.sql`
  - `V7__insert_default_roles_permissions.sql` (seed data)
- `[x] Viết Flyway migration monolith_db:`
  - `V1__create_user_profiles_table.sql`
  - `V2__create_products_table.sql`
  - `V3__create_orders_order_items_tables.sql`
  - `V4__create_payments_table.sql`
  - `V5__create_audit_logs_table.sql`
- `[x] Viết Flyway migration order_db, product_db, user_db, payment_db, notification_db, file_db cho Microservice`

**Definition of Done:** Chạy Flyway migrate thành công, schema đúng với thiết kế ở Architecture Bible.

---

## Phase 1: SSO Server (Sprint 02-05)

### Sprint 02 — SSO Server Bootstrap & OAuth2 Foundation
**Mục tiêu:** SSO Server khởi động, expose JWKS endpoint, cấu hình OAuth2 clients.

- `[x] Khởi tạo sso-server Spring Boot app với các dependencies chính`
- `[x] Implement AuthorizationServerConfig.java:`
  - Đăng ký 2 OAuth2 clients: `monolith-web` và `microservice-gateway`
  - Đăng ký các service clients (Client Credentials): `order-service`, `payment-service`, v.v.
  - Cấu hình `RegisteredClientRepository` lưu vào PostgreSQL (`OauthClient` entity + `JpaRegisteredClientRepository` custom mapper)
  - Token settings: access token TTL = 15 phút, refresh token TTL = 7 ngày
- `[x] Implement RSA key pair generation và JWKS endpoint:`
  - Tạo `KeyPairConfig.java` generate RSA-2048 key pair
  - Expose `/.well-known/openid-configuration`
  - Expose `/oauth2/jwks`
- `[x] Implement JwtCustomizerConfig.java:`
  - Thêm custom claims vào JWT: `roles`, `permissions`, `email`, `name`
  - Load roles và permissions từ DB của user qua Principal
- `[x] Implement CustomUserDetailsService.java:`
  - Load user từ PostgreSQL qua `UserRepository`
  - Check `enabled` và `locked` status thông qua `SsoUserDetails` bọc ngoài

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
- `[x]` Implement `UserEntity.java` với các fields:
  ```java
  id (UUID), username, email, passwordHash, firstName, lastName,
  enabled, locked, createdAt, updatedAt, lastLoginAt
  ```
- `[x]` Implement `RoleEntity.java` và `PermissionEntity.java` với ManyToMany relationship
- `[x]` Implement `AdminUserController.java`:
  - `POST /admin/users` — Tạo user mới (chỉ ADMIN)
  - `GET /admin/users` — Danh sách users có phân trang
  - `GET /admin/users/{id}` — Chi tiết user
  - `PUT /admin/users/{id}` — Cập nhật thông tin
  - `PUT /admin/users/{id}/status` — Enable/Disable user
  - `POST /admin/users/{id}/roles` — Gán roles
  - `DELETE /admin/users/{id}/roles/{roleId}` — Thu hồi role
- `[x]` Implement `AdminRoleController.java`:
  - `POST /admin/roles` — Tạo role mới
  - `GET /admin/roles` — Danh sách roles
  - `POST /admin/roles/{id}/permissions` — Gán permissions
- `[x]` Bảo mật tất cả `/admin/*` endpoints: chỉ ADMIN service account được phép gọi (Client Credentials)
- `[x]` Viết Unit Test cho UserService và RoleService (Mockito)

**Definition of Done:** CRUD User/Role/Permission API hoạt động, có Javadoc đầy đủ tiếng Việt.

---

### Sprint 04 — Authentication Flow: Login, Logout, Password Change
**Mục tiêu:** Hoàn thiện các luồng xác thực cốt lõi.

**Tasks:**
- `[x]` Implement `BruteForceProtectionService.java`:
  ```java
  // Dùng Redis: login:attempt:{username} → TTL 5 phút
  // Sau 5 lần thất bại → lock account tạm thời
  // Sau 10 lần → lock vĩnh viễn, cần Admin unlock
  ```
- `[x]` Implement `AuthController.java`:
  - `POST /auth/change-password` — Đổi mật khẩu (yêu cầu old password)
- `[x]` Implement Two-Factor Authentication (2FA / TOTP):
  - Setup flow: `/auth/2fa/setup` sinh secret key (Base32) và QR Code URL
  - Verification flow: `/auth/2fa/verify` để kích hoạt
  - Verification during Login: Bắt buộc nhập OTP 6 số nếu user đã enable 2FA
- `[x]` Custom giao diện (HTML/CSS templates) cho các trang Login, Consent, và 2FA Verification trên SSO Server để đồng bộ thương hiệu
- `[x]` Viết Unit Tests & Integration Tests cho luồng xác thực nâng cao và brute force protection

**Definition of Done:** Login → lấy được token. Đăng nhập sai 5 lần → bị lock. Đổi refresh token → token cũ bị invalidate. Xác thực 2FA hoạt động chính xác. Giao diện Login/2FA custom hiển thị đồng bộ.

---

### Sprint 05 — SSO Cross-App Session (Single Sign-On)
**Mục tiêu:** Chứng minh SSO thực sự hoạt động giữa Monolith và Microservice App.

**Tasks:**
- `[x]` Cấu hình SSO Server session persistence với Redis (không dùng in-memory)
  - Lý do: Trong môi trường nhiều SSO Server instances, session phải shared
- `[x]` Test SSO flow đầy đủ:
  1. Login vào Monolith App → SSO redirect về Monolith
  2. Mở Microservice App → SSO phát hiện session đã tồn tại → không hỏi password lại
  3. Logout khỏi một app → Cả 2 app bị logout
- `[x]` Implement Back-Channel Logout (SSO notify apps khi user logout):
  - SSO gọi `POST {app}/logout` của từng registered client
- `[x]` Document toàn bộ flow bằng Sequence Diagram trong `docs/sequences/`
- `[x]` Viết Security Test: JWT tampering, expired token, wrong issuer

**Definition of Done:** SSO hoạt động giữa 2 app. Logout từ app A → app B cũng bị logout.

---

## Phase 2: Monolith App (Sprint 06-10)

### Sprint 06 — Monolith Bootstrap & OAuth2 Client Integration
**Mục tiêu:** Monolith App khởi động, tích hợp SSO login, nhận được JWT.

**Tasks:**
- `[x]` Khởi tạo `monolith-app` Spring Boot với dependencies:
  - `spring-boot-starter-security`
  - `spring-boot-starter-oauth2-client` (OAuth2 Login)
  - `spring-boot-starter-oauth2-resource-server` (JWT validation)
  - `spring-boot-starter-data-jpa`
- `[x]` Implement `SecurityConfig.java`:
  ```java
  @Configuration
  @EnableWebSecurity
  @EnableMethodSecurity  // Bật @PreAuthorize, @PostAuthorize
  public class SecurityConfig {
      // oauth2Login → redirect đến SSO
      // oauth2ResourceServer → validate JWT từ JWKS của SSO
  }
  ```
- `[x]` Implement `SsoJwtGrantedAuthoritiesConverter.java`:
  - Đọc `roles` và `permissions` từ JWT claims
  - Convert thành `SimpleGrantedAuthority` để `@PreAuthorize` hoạt động
- `[x]` Implement `UserProfileEntity.java` (chứa extended profile info, FK = SSO user UUID)
- `[x]` Implement `UserProfileController.java`:
  - `GET /api/users/me` — Lấy profile của chính mình
  - `PUT /api/users/me` — Cập nhật profile
- `[x]` Cấu hình CORS cho `http://localhost:3000` (Frontend dev server)
- `[x]` Cấu hình Spring Security Resource Server OIDC & Jwt validation chéo.
- `[x]` Viết Unit Test cho JWT converter class.

**Definition of Done:** `GET /api/users/me` trả về profile thành công. CORS cho `http://localhost:3000` (Monolith Frontend) và `http://localhost:3001` (Microservice Frontend) được cấu hình chính xác. Unit test JWT converter pass.

---

### Sprint 07 — Monolith Product & @PreAuthorize
**Mục tiêu:** CRUD Product với phân quyền `@PreAuthorize` đầy đủ.

**Tasks:**
- `[x]` Implement `ProductEntity.java`, `ProductRepository.java`
- `[x]` Implement `ProductService.java` với `@PreAuthorize` **ở Service Layer**:
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
- `[x]` Implement `ProductController.java` (không có `@PreAuthorize` ở Controller)
- `[x]` Viết Unit Test cho từng security scenario:
  - USER gọi `createProduct` → `AccessDeniedException`
  - STAFF gọi `createProduct` → thành công
  - USER gọi `deleteProduct` → `AccessDeniedException`
  - ADMIN gọi `deleteProduct` → thành công
- `[x]` Cấu hình H2 Database độc lập cho các integration/security tests.
- `[x]` Triển khai Request/Response validation chặt chẽ cho Product APIs.

**Definition of Done:** Tất cả unit test backend pass. `@PreAuthorize` hoạt động chính xác: USER → 403 khi tạo/xóa sản phẩm, ADMIN → 200. API response đúng format `ApiResponse<T>`.

---

### Sprint 08 — Monolith Order Service & Resource Ownership
**Mục tiêu:** Implement ABAC (Resource Ownership) — user chỉ xem/hủy đơn hàng của chính mình.

**Tasks:**
- `[x]` Implement `OrderEntity.java`, `OrderItemEntity.java`, `OrderRepository.java`
- `[x]` Implement `OrderSecurityEvaluator.java` (Spring Bean cho SpEL):
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
- `[x]` Implement `OrderService.java`:
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
- `[x]` Implement `PaymentService.java` (Mock Sandbox):
  ```java
  @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
  public PaymentResponse processPayment(ProcessPaymentRequest req) { ... }
  ```
- `[x]` Viết Integration Test: test ownership violation → HTTP 403 (Pass)
- `[x]` Triển khai SpEL parameter mapping bằng annotation `@P("orderId")` để đảm bảo tương thích chéo an toàn.
- `[x]` Triển khai cấu trúc dọn dẹp H2 DB theo thứ tự ràng buộc khóa ngoại chéo giữa các integration tests.

**Definition of Done:** USER không thể xem đơn hàng của người khác → 403. ADMIN có thể xem tất cả. Idempotency Key chống duplicate order. Integration test pass.

---

### Sprint 09 — Monolith Audit Log & Security Hardening
**Mục tiêu:** Audit log tự động, bảo mật nâng cao cho Monolith.

**Tasks:**
- `[x]` Implement `@Auditable` annotation:
  ```java
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Auditable {
      String action();       // Ví dụ: "ORDER_CREATED"
      String resource();     // Ví dụ: "Order"
  }
  ```
- `[x]` Implement `AuditLogAspect.java` (Spring AOP):
  - Intercept `@Auditable` methods
  - Ghi audit log: `actor_id`, `action`, `resource`, `resource_id`, `ip`, `timestamp`
  - Log cả thành công lẫn thất bại (sử dụng `@Around` advice và Order ưu tiên cao nhất)
- `[x]` Security Headers cấu hình tại `SecurityConfig`:
  - `X-Frame-Options: DENY` (chống Clickjacking)
  - `X-Content-Type-Options: nosniff` (chống MIME sniffing)
  - `Content-Security-Policy: default-src 'self'`
  - `Strict-Transport-Security: max-age=31536000` (HSTS)
- `[x]` Viết Security Attack Test:
  - Test JWT với `alg:none` → bị từ chối
  - Test JWT với wrong issuer → HTTP 401
  - Test JWT với wrong audience → HTTP 401
  - Test tampered JWT payload → HTTP 401
- `[x]` Triển khai MockMvc HTTPS simulation (`.secure(true)`) để kiểm tra HSTS headers chính xác.
- `[x]` Triển khai cơ chế lấy `@Auditable` annotation động qua reflection chéo tránh lỗi JoinPointMatch parameter binding.

**Definition of Done:** Tất cả security tests pass. Audit logs được ghi đầy đủ với đầy đủ thông tin actor, action, entity, IP. Security headers được inject vào tất cả response.

---

### Sprint 10 — Monolith Complete Integration Test
**Mục tiêu:** Integration test end-to-end cho toàn bộ Monolith Backend.

**Tasks:**
- `[x]` Setup H2 in-memory database (PostgreSQL mode) cho Integration Test (không cần Docker)
- `[x]` Viết `MonolithCompleteIntegrationTest.java` với các scenarios:
  - Scenario 1 — Happy path: JWT hợp lệ → GET /products → POST /orders → GET /orders/{id} thành công
  - Scenario 2 — Auth failure: JWT hết hạn → 401 Unauthorized
  - Scenario 3 — Authorization failure: USER role → DELETE /products → 403 Forbidden
  - Scenario 4 — Ownership violation: USER A → GET /orders/{orderId of User B} → 403 Forbidden
- `[x]` Viết `SsoBruteForceIntegrationTest.java`:
  - Scenario 5 — Brute force: 6 lần login sai → account locked tạm thời → 10 lần → locked vĩnh viễn
- `[ ]` Load test nhỏ với k6: 100 concurrent users, 60 seconds → P99 < 500ms
  - Viết script `monolith-app/src/test/resources/k6/monolith-load-test.js`

**Definition of Done:** Tất cả 5 Integration Test scenarios backend pass (`BUILD SUCCESS`). k6 load test script sẵn sàng.

---

## Phase 2.5: Monolith Frontend — React.js + Vite (Sprint 10.5)

### Sprint 10.5 — Monolith Frontend Bootstrap & OAuth2 PKCE Login
**Mục tiêu:** Khởi tạo ứng dụng React.js SPA, implement luồng OAuth2 Authorization Code + PKCE để đăng nhập qua SSO Server.

**Tại sao React.js (không Next.js)?**
- Monolith là ứng dụng **Client-Side Rendered (SPA)** — không cần SSR vì Monolith Backend đã xử lý data
- Học cách tự implement PKCE flow từ đầu (giá trị học tập cao hơn)
- Vite cho hot reload nhanh, bundle nhỏ

**Cơ chế SSO Login cho React SPA:**
```
1. User bấm "Đăng nhập" → Frontend generate code_verifier + code_challenge (SHA-256)
2. Redirect sang: http://sso-server:9000/oauth2/authorize
      ?client_id=monolith-web
      &response_type=code
      &redirect_uri=http://localhost:3000/callback
      &scope=openid profile email
      &code_challenge=BASE64URL(SHA256(code_verifier))
      &code_challenge_method=S256
      &state=random_csrf_token
3. User thấy trang Login tùy chỉnh của SSO Server → nhập username/password
4. SSO redirect về: http://localhost:3000/callback?code=AUTH_CODE&state=xxx
5. React /callback page gọi POST /oauth2/token với code + code_verifier
6. Nhận: { access_token, refresh_token, expires_in } → lưu vào sessionStorage
7. Gọi API Monolith với header: Authorization: Bearer {access_token}
```

**Cấu trúc project:**
```
monolith-frontend/
├── src/
│   ├── auth/
│   │   ├── pkce.ts           # generate code_verifier, code_challenge
│   │   ├── oauth.ts          # buildAuthorizeUrl, exchangeCode, refreshToken
│   │   └── AuthContext.tsx   # React Context lưu token + user info
│   ├── components/
│   │   ├── common/           # Button, Input, Modal, Badge, Table, Skeleton
│   │   └── layout/           # Sidebar, Header, DashboardLayout, ProtectedRoute
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── CallbackPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── products/
│   │   │   ├── ProductListPage.tsx
│   │   │   ├── ProductDetailPage.tsx
│   │   │   └── ProductFormPage.tsx
│   │   ├── orders/
│   │   │   ├── OrderListPage.tsx
│   │   │   ├── OrderDetailPage.tsx
│   │   │   └── CheckoutPage.tsx
│   │   ├── ProfilePage.tsx
│   │   └── admin/
│   │       ├── AdminUsersPage.tsx
│   │       └── AuditLogPage.tsx
│   ├── services/
│   │   ├── apiClient.ts      # fetch wrapper với auto Bearer token
│   │   ├── productApi.ts
│   │   ├── orderApi.ts
│   │   └── userApi.ts
│   ├── hooks/
│   │   ├── useAuth.ts        # access token, user info, logout
│   │   └── usePermission.ts  # check permission từ JWT claims
│   ├── types/
│   │   ├── api.ts            # ApiResponse<T>, PageResponse<T>
│   │   ├── auth.ts           # TokenResponse, UserInfo
│   │   ├── product.ts
│   │   └── order.ts
│   ├── styles/
│   │   └── globals.css       # CSS Variables (dark theme)
│   ├── App.tsx               # React Router setup
│   └── main.tsx
├── index.html
├── vite.config.ts            # proxy /api → http://localhost:8080
├── tsconfig.json
└── package.json
```

**Tasks:**
- `[x]` Khởi tạo project: `npm create vite@latest monolith-frontend -- --template react-ts`
  - Cài dependencies: `react-router-dom`, `@types/node`
  - Cấu hình Vite proxy: `/api/**` → `http://localhost:8080`
  - Cấu hình absolute imports: `@/` alias
- `[x]` Implement `src/auth/pkce.ts`:
  - `generateCodeVerifier()`: 128 byte random string, base64url encoded
  - `generateCodeChallenge(verifier)`: SHA-256 hash, base64url encoded
- `[x]` Implement `src/auth/oauth.ts`:
  - `buildAuthorizeUrl(state, codeChallenge)`: tạo URL redirect sang SSO
  - `exchangeCode(code, codeVerifier)`: POST `/oauth2/token` lấy tokens
  - `refreshAccessToken(refreshToken)`: đổi refresh token lấy access token mới
  - `revokeToken(token)`: POST `/oauth2/revoke` khi logout
- `[x]` Implement `src/auth/AuthContext.tsx`:
  - Lưu `accessToken`, `refreshToken`, `userInfo` (decode từ JWT payload)
  - Auto refresh khi token sắp hết hạn (silent renew trước 60 giây)
  - `useAuth()` hook export ra `{ user, login, logout, hasPermission }`
- `[x]` Implement `src/styles/globals.css` (Design Tokens)
  - Dark theme: bg `#09090b`, brand `#6366f1`, accent `#8b5cf6`
  - Typography: Inter font (Google Fonts)
- `[x]` Implement **Layout Components** (CSS Modules):
  - `Sidebar.tsx`: Navigation links ẩn/hiện theo role
  - `Header.tsx`: Avatar, tên user, role badge, nút Logout
  - `ProtectedRoute.tsx`: Redirect về `/login` nếu chưa auth
- `[x]` Implement **LoginPage** (`/login`):
  - Màn hình Welcome với logo SSO Platform
  - Nút "Đăng nhập qua SSO" → gọi `oauth.buildAuthorizeUrl()` → redirect
- `[x]` Implement **CallbackPage** (`/callback`):
  - Đọc `?code=` và `?state=` từ URL
  - Validate state chống CSRF
  - Gọi `oauth.exchangeCode()` → lưu tokens → redirect về `/dashboard`
- `[x]` Implement **Product Module**:
  - `ProductListPage`: Bảng danh sách, phân trang, filter theo category
  - `ProductDetailPage`: Chi tiết sản phẩm, ảnh, giá, số lượng tồn kho
  - `ProductFormPage`: Form tạo/sửa — chỉ hiện với PRODUCT_CREATE/UPDATE permission
  - Nút "Xóa" chỉ hiện với ADMIN/MANAGER
- `[x]` Implement **Order Module**:
  - `CheckoutPage`: Chọn sản phẩm, nhập địa chỉ, gửi kèm `Idempotency-Key` UUID
  - `OrderListPage`: ADMIN thấy tất cả đơn, USER chỉ thấy của mình
  - `OrderDetailPage`: Chi tiết + nút "Hủy đơn" nếu status PENDING
- `[x]` Implement **Profile Page** (`/profile`):
  - Hiển thị thông tin, role badge
  - Form đổi mật khẩu (gọi `POST /auth/change-password` trên SSO Server)
- `[x]` Implement **Admin Module** (chỉ ADMIN thấy trong Sidebar):
  - `AdminUsersPage`: Bảng user, nút Enable/Disable, nút Gán Role
  - `AuditLogPage`: Bảng audit log, filter theo actor, action, date range
- `[x]` Xử lý **Token Expiry & Auto-refresh**:
  - Interceptor trong `apiClient.ts`: nếu 401 → thử refresh → retry request
  - Nếu refresh thất bại → logout + redirect `/login`
- `[x]` **E2E Test với Playwright** (sau khi có UI):
  - `tests/happy-path.spec.ts`: Login → Xem sản phẩm → Checkout → Xem đơn hàng → Logout
  - Cấu hình `playwright.config.ts` cho môi trường dev local

**Definition of Done:** User bấm Đăng nhập → redirect SSO → nhập pass → về Dashboard hiển thị đúng tên/role. Products/Orders render đúng dữ liệu. Nút CRUD hiển thị/ẩn đúng permission. Logout hoạt động, revoke token. Playwright E2E happy path pass.

---

## Phase 2.6: Microservice Frontend — Next.js 15 + NextAuth.js (Sprint 10.6)

### Sprint 10.6 — Microservice Frontend Bootstrap & NextAuth SSO Integration
**Mục tiêu:** Khởi tạo ứng dụng Next.js 15 (App Router, RSC), tích hợp NextAuth.js v5 để đăng nhập qua SSO Server tự động, xây dựng UI đầy đủ cho Microservice App.

**Tại sao Next.js 15 (không React)?**
- Microservice App phức tạp hơn — cần **Server-Side Rendering** cho SEO và performance
- **React Server Components (RSC)** fetch data ở server → không lộ token ra browser
- **NextAuth.js v5** xử lý toàn bộ OAuth2 flow + token rotation tự động
- Phù hợp với production-grade app (không phải prototype)

**Cơ chế SSO Login với NextAuth.js:**
```
1. User vào /dashboard → middleware kiểm tra session → chưa có → redirect /login
2. User bấm "Đăng nhập" → gọi signIn('sso-server') của NextAuth
3. NextAuth tự động:
   a. Generate PKCE code_verifier, code_challenge
   b. Redirect sang SSO Server /oauth2/authorize
   c. Sau khi login, SSO redirect về /api/auth/callback/sso-server
   d. NextAuth exchange code → lấy tokens
   e. Lưu tokens vào HTTP-only cookie (XSS-safe)
   f. Redirect về /dashboard
4. Server Components đọc session từ cookie (server-side) → fetch API Gateway
5. Client Components dùng useSession() hook
6. Token sắp hết hạn → NextAuth tự động refresh (jwt callback)
```

**Cấu trúc project:**
```
microservice-frontend/
├── src/
│   ├── app/
│   │   ├── layout.tsx              # Root layout (SessionProvider, fonts)
│   │   ├── page.tsx                # Landing → redirect to /dashboard
│   │   ├── (auth)/
│   │   │   └── login/
│   │   │       └── page.tsx        # Trang Login: nút "Đăng nhập qua SSO"
│   │   ├── (dashboard)/
│   │   │   ├── layout.tsx          # RSC: kiểm tra session, render Sidebar+Header
│   │   │   ├── page.tsx            # Dashboard overview
│   │   │   ├── products/
│   │   │   │   ├── page.tsx        # RSC: fetch /api/products từ Gateway
│   │   │   │   ├── new/page.tsx    # Client: form tạo sản phẩm
│   │   │   │   └── [id]/page.tsx   # RSC: chi tiết sản phẩm
│   │   │   ├── orders/
│   │   │   │   ├── page.tsx        # RSC: danh sách đơn hàng
│   │   │   │   ├── new/page.tsx    # Client: checkout form
│   │   │   │   └── [id]/page.tsx   # RSC: chi tiết đơn hàng
│   │   │   ├── profile/
│   │   │   │   └── page.tsx        # Client: profile + avatar + 2FA setup
│   │   │   └── admin/
│   │   │       ├── users/page.tsx  # RSC: danh sách users (ADMIN only)
│   │   │       ├── services/page.tsx # RSC: Eureka health dashboard
│   │   │       └── reports/page.tsx  # Client: biểu đồ doanh thu (Chart.js)
│   │   └── api/
│   │       └── auth/
│   │           └── [...nextauth]/
│   │               └── route.ts    # NextAuth API handler
│   ├── auth.ts                     # NextAuth config (SSO provider, callbacks)
│   ├── middleware.ts               # Bảo vệ routes: check session
│   ├── components/
│   │   ├── common/                 # Button, Input, Modal, Badge, Table, Skeleton
│   │   └── layout/                 # Sidebar, Header, DashboardShell
│   ├── lib/
│   │   ├── api/
│   │   │   ├── client.ts           # fetch wrapper (server-side dùng token)
│   │   │   ├── products.ts
│   │   │   ├── orders.ts
│   │   │   └── users.ts
│   │   └── utils.ts
│   ├── types/
│   │   ├── api.ts
│   │   ├── auth.ts                 # Session, CurrentUser
│   │   ├── product.ts
│   │   └── order.ts
│   └── styles/
│       └── globals.css             # CSS Variables (dark theme)
├── next.config.ts
├── .env.local                      # gitignored
├── .env.example                    # committed
├── tsconfig.json
└── package.json
```

**Tasks:**
- `[ ]` Khởi tạo project:
  ```bash
  npx create-next-app@latest microservice-frontend \
    --typescript --app --src-dir --no-tailwind --import-alias "@/*"
  ```
  - Cài dependencies: `next-auth@beta`, `chart.js`, `react-chartjs-2`
- `[ ]` Cấu hình `.env.local` và `.env.example`:
  ```env
  AUTH_SECRET=          # openssl rand -base64 32
  AUTH_ISSUER=http://sso-server:9000
  AUTH_CLIENT_ID=microservice-gateway
  AUTH_CLIENT_SECRET=
  NEXT_PUBLIC_API_URL=http://localhost:8090
  ```
- `[ ]` Implement `src/auth.ts` — NextAuth v5 config:
  - Custom OAuth2 Provider trỏ đến SSO Server endpoints
  - `jwt` callback: lưu `access_token`, `refresh_token`, `expires_at` vào JWT
  - `session` callback: expose `accessToken` + `user.roles` + `user.permissions` cho client
  - Auto-refresh: nếu `expires_at < Date.now()` → gọi `/oauth2/token` với `refresh_token`
- `[ ]` Implement `src/middleware.ts`:
  - Dùng NextAuth `auth` middleware
  - Public routes: `/login`, `/api/auth/**`
  - Protected routes: tất cả còn lại → redirect `/login` nếu chưa auth
- `[ ]` Implement `src/app/api/auth/[...nextauth]/route.ts`:
  - NextAuth handler xử lý SSO callback
- `[ ]` Implement `src/styles/globals.css` (cùng Design Tokens với monolith-frontend)
- `[ ]` Implement **Layout Components** (CSS Modules):
  - `Sidebar.tsx`: Server Component đọc session, render nav links theo role
  - `Header.tsx`: Avatar, tên user, role badge, nút Logout (`signOut()`)
  - `(dashboard)/layout.tsx`: wrap content với DashboardShell
- `[ ]` Implement **Login Page** (`/login`):
  - Màn hình Welcome đồng bộ thương hiệu với Monolith Frontend
  - Nút "Đăng nhập qua SSO" → `signIn('sso-server')` → NextAuth tự redirect
- `[ ]` Implement **Product Module** (RSC + Client):
  - `products/page.tsx` (RSC): fetch từ API Gateway kèm `Authorization: Bearer {serverToken}`
  - `products/new/page.tsx` (Client): form tạo sản phẩm, submit với `useTransition`
  - `products/[id]/page.tsx` (RSC): chi tiết sản phẩm
  - Render nút CRUD dựa trên `session.user.permissions`
- `[ ]` Implement **Order Module** (RSC + Client):
  - `orders/new/page.tsx` (Client): Checkout form gửi kèm `Idempotency-Key: crypto.randomUUID()`
  - `orders/page.tsx` (RSC): danh sách đơn hàng
  - `orders/[id]/page.tsx` (RSC): chi tiết + nút Hủy
- `[ ]` Implement **Profile Page** (Client Component):
  - Hiển thị thông tin user, role badges
  - Upload avatar: `POST /api/files/upload` (multipart) → MinIO → cập nhật URL
  - Bật 2FA: gọi `GET /auth/2fa/setup` → nhận QR Code URL → hiển thị modal QR
  - Nhập OTP 6 số → `POST /auth/2fa/verify` để kích hoạt
- `[ ]` Implement **Admin Module** (RSC, chỉ ADMIN thấy trong Sidebar):
  - `admin/users/page.tsx`: Bảng user + phân trang + nút Enable/Disable + Gán Role modal
  - `admin/services/page.tsx`: Fetch Eureka API → hiển thị danh sách services và health status
  - `admin/reports/page.tsx` (Client): Chart.js biểu đồ doanh thu theo ngày/tháng
- `[ ]` Implement **Error Handling UI**:
  - `error.tsx` (Client): Error boundary hiển thị thông báo thân thiện
  - Fallback UI khi API Gateway trả 503 (Circuit Breaker OPEN): "Hệ thống đang bận..."
  - Toast notifications cho success/error actions
- `[ ]` **E2E Test với Playwright**:
  - `tests/happy-path.spec.ts`: Login → Xem sản phẩm → Checkout → Xem đơn → Logout
  - `tests/admin.spec.ts`: Login ADMIN → Xem users → Disable user → Xem audit log
  - Cấu hình `playwright.config.ts` cho `http://localhost:3001`

**Definition of Done:** User vào `/dashboard` → redirect `/login` → Đăng nhập SSO → về Dashboard. RSC fetch và render đúng dữ liệu từ API Gateway. Token tự động refresh sau 15 phút. Profile + Avatar upload hoạt động. 2FA setup thành công. Admin dashboard hiển thị service health. Playwright E2E happy path pass.

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
- `[ ]` Cấu hình `.env.local` của `microservice-frontend` chuyển hướng `NEXT_PUBLIC_API_URL` sang API Gateway (`:8090`)
- `[ ]` Cập nhật `apiClient.ts` trong microservice-frontend để tự động đính kèm Access Token từ NextAuth session vào header `Authorization: Bearer`

**Definition of Done:** Request với valid JWT → Gateway inject headers → forward đến service. Invalid JWT → 401. Fake X-User headers từ client → bị strip. microservice-frontend gọi thành công API qua cổng Gateway.

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

| Phase | Sprints | Framework | Mục Tiêu Chính |
|---|---|---|---|
| Phase 0 | 00-01 | — | Infrastructure, DB Schema, Common Library |
| Phase 1 | 02-05 | Spring Authorization Server | SSO Server: OAuth2, OIDC, JWT, RBAC |
| Phase 2 | 06-10 | Spring Boot + Spring Security | Monolith App: @PreAuthorize, ABAC, Audit Log |
| **Phase 2.5** | **10.5** | **React.js 18 + Vite + TypeScript** | **Monolith Frontend: PKCE OAuth2, Products, Orders, Admin** |
| **Phase 2.6** | **10.6** | **Next.js 15 + NextAuth.js v5** | **Microservice Frontend: RSC, SSO auto-login, 2FA, Reports** |
| Phase 3 | 11-17 | Spring Cloud Gateway + Microservices | Microservice: Gateway, Services, Kafka, Resilience4j |
| Phase 4 | 18-21 | — | Security Testing, Key Rotation, Distributed Tracing |
| Phase 5 | 22-25 | Docker + GitLab CI + Kubernetes | Observability, CI/CD, Documentation |

---

## Ghi Chú Thiết Kế Frontend

### Tại sao 2 Framework khác nhau?

| Điểm | Monolith Frontend (React.js) | Microservice Frontend (Next.js) |
|---|---|---|
| **Rendering** | Client-Side Rendering (SPA) | Server-Side + RSC |
| **OAuth2 Flow** | Tự implement PKCE (học sâu) | NextAuth.js tự động hóa |
| **Token Storage** | `sessionStorage` (in-memory) | HTTP-only Cookie (NextAuth) |
| **API Target** | Monolith `:8080` | API Gateway `:8090` |
| **Mục đích học** | Hiểu OAuth2 raw flow | Hiểu production SSO pattern |

### Luồng SSO Login tổng quát

```
Browser                    Frontend App              SSO Server (:9000)
   │                           │                           │
   │── bấm "Đăng nhập" ───────>│                           │
   │                           │── redirect ──────────────>│
   │                           │   /oauth2/authorize        │
   │<── SSO Login page ────────────────────────────────────│
   │── nhập username/password ─────────────────────────────>│
   │<── redirect với ?code=... ────────────────────────────│
   │── GET /callback?code=... ─>│                           │
   │                           │── POST /oauth2/token ─────>│
   │                           │<── access_token, refresh ──│
   │<── redirect /dashboard ───│                           │
   │                           │                           │
   │── GET /api/products ──────>│── Bearer token ──> Backend│
```
