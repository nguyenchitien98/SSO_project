# SSO Platform - Backlog & Business Flows

Tài liệu này tổng hợp các User Stories, API Contracts và Business Flows của dự án **SSO Platform**.

---

## 1. Tổng Hợp User Stories

### 1.1 Authentication (SSO Server)

| ID | User Story | Priority | Sprint |
|---|---|---|---|
| AUTH-01 | Là user, tôi muốn đăng nhập 1 lần và truy cập được cả Monolith và Microservice App | HIGH | 05 |
| AUTH-02 | Là user, tôi muốn đổi mật khẩu bất cứ lúc nào | HIGH | 04 |
| AUTH-03 | Là user, tôi muốn đăng xuất khỏi tất cả apps cùng lúc | HIGH | 05 |
| AUTH-04 | Là user, tôi không muốn bị lock account vĩnh viễn khi quên mật khẩu | MED | 04 |
| AUTH-05 | Là admin, tôi muốn xem danh sách active sessions của một user | HIGH | 04 |
| AUTH-06 | Là admin, tôi muốn force logout một user cụ thể (thu hồi toàn bộ token) | HIGH | 04 |

### 1.2 Authorization — Monolith

| ID | User Story | Priority | Sprint |
|---|---|---|---|
| MONO-01 | Là STAFF, tôi muốn tạo và sửa sản phẩm, nhưng không được xóa | HIGH | 07 |
| MONO-02 | Là MANAGER, tôi muốn xóa sản phẩm nhưng không muốn quản lý users | HIGH | 07 |
| MONO-03 | Là USER, tôi chỉ muốn xem đơn hàng của chính mình | HIGH | 08 |
| MONO-04 | Là ADMIN, tôi muốn xem và quản lý tất cả đơn hàng | HIGH | 08 |
| MONO-05 | Là USER, tôi muốn hủy đơn hàng chưa được xác nhận của mình | MED | 08 |
| MONO-06 | Là AUDITOR, tôi muốn xem audit logs nhưng không thay đổi gì | MED | 09 |

### 1.3 Authorization — Microservice

| ID | User Story | Priority | Sprint |
|---|---|---|---|
| MS-01 | Là user, khi gọi API qua Gateway với JWT hợp lệ → truy cập được | HIGH | 11 |
| MS-02 | Là user, khi gọi API với JWT expired → nhận 401 rõ ràng | HIGH | 11 |
| MS-03 | Là attacker, khi tôi inject X-User-Id header → bị reject | HIGH | 11 |
| MS-04 | Là USER, gọi DELETE /products → nhận 403 Forbidden | HIGH | 13 |
| MS-05 | Là USER, GET /orders/{orderId của người khác} → nhận 403 | HIGH | 14 |
| MS-06 | Là Order Service, gọi Payment Service → chỉ work với service token | HIGH | 15 |
| MS-07 | Là user, tôi muốn kích hoạt/vô hiệu hóa 2FA/TOTP để bảo mật tài khoản | HIGH | 04 |
| MS-08 | Là user, khi đăng nhập với 2FA được bật, tôi phải nhập mã OTP 6 số để xác thực | HIGH | 04 |
| MS-09 | Là user, tôi muốn tải lên ảnh đại diện (avatar) dung lượng dưới 5MB dạng ảnh | MED | 13 |
| MS-10 | Là admin, tôi muốn xem trạng thái sức khỏe (health checks) và số instances của các service | HIGH | 12 |
| MS-11 | Là manager, tôi muốn xem biểu đồ doanh thu, số lượng đơn hàng, và tài nguyên hệ thống | MED | 22 |

---

## 2. API Contracts

### 2.1 SSO Server APIs

```
# OIDC Discovery
GET  /.well-known/openid-configuration
→ 200: OIDC metadata JSON

GET  /oauth2/jwks
→ 200: JWKS JSON (public keys)

# OAuth2 Authorization
GET  /oauth2/authorize
     ?client_id=monolith-web
     &response_type=code
     &redirect_uri=http://localhost:8080/login/oauth2/code/sso
     &scope=openid profile email
     &code_challenge=<pkce>
     &code_challenge_method=S256
     &state=<random>
→ 302: Redirect to login page

POST /oauth2/token
Body: grant_type=authorization_code
      &code=<code>
      &redirect_uri=...
      &client_id=monolith-web
      &code_verifier=<pkce_verifier>
→ 200: { access_token, refresh_token, id_token, expires_in, token_type }

POST /oauth2/token (Client Credentials — service-to-service)
Body: grant_type=client_credentials
      &client_id=order-service
      &client_secret=...
      &scope=payment:write
→ 200: { access_token, expires_in, token_type }

POST /oauth2/revoke
Body: token=<token>&token_type_hint=refresh_token
→ 200: OK

# Auth Management
POST /auth/change-password
     X-User-Id: {uuid}
Body: { oldPassword, newPassword }
→ 200 | 400 (weak password) | 403 (wrong old password)

POST /auth/2fa/setup
     Authorization: Bearer {token}
→ 200: { secretKey, qrCodeUrl }

POST /auth/2fa/verify
     Authorization: Bearer {token}
Body: { otpCode }
→ 200: { enabled: true, backupCodes: [...] } | 400

POST /oauth2/verify-2fa
Body: { preAuthToken, otpCode }
→ 200: { access_token, refresh_token, id_token } | 400

POST /auth/logout
     Authorization: Bearer {token}
→ 200: { message: "Đăng xuất thành công" }

# Admin APIs (Client Credentials của admin-client)
GET  /admin/users?page=0&size=20
→ 200: PageResponse<UserSummary>

POST /admin/users
Body: { username, email, password, roles }
→ 201: UserDetail

GET  /admin/users/{id}
→ 200: UserDetail | 404

PUT  /admin/users/{id}/status
Body: { enabled: true | false, reason: "..." }
→ 200

POST /admin/users/{id}/roles
Body: { role: "MANAGER" }
→ 200

DELETE /admin/users/{id}/roles/{role}
→ 204

GET  /admin/audit-logs?userId=&action=&page=0&size=20
→ 200: PageResponse<AuditLog>
```

### 2.2 Monolith App APIs

```
# Products
GET  /api/products?page=0&size=10&keyword=&category=
     (Public — không cần auth)
→ 200: ApiResponse<PageResponse<ProductResponse>>

GET  /api/products/{id}
     (Public)
→ 200: ApiResponse<ProductResponse> | 404

POST /api/products
     Authorization: Bearer {token}  (cần PRODUCT_CREATE permission)
Body: CreateProductRequest
→ 201: ApiResponse<ProductResponse> | 400 | 403

PUT  /api/products/{id}
     Authorization: Bearer {token}  (cần PRODUCT_UPDATE)
Body: UpdateProductRequest
→ 200: ApiResponse<ProductResponse> | 404 | 403

DELETE /api/products/{id}
     Authorization: Bearer {token}  (cần ADMIN hoặc MANAGER role)
→ 204 | 403 | 404

# Orders
POST /api/orders
     Authorization: Bearer {token}
     Idempotency-Key: {uuid}
Body: CreateOrderRequest
→ 201: ApiResponse<OrderResponse> | 400 | 409 (duplicate)

GET  /api/orders/{id}
     Authorization: Bearer {token}
→ 200: ApiResponse<OrderResponse>
     (403 nếu USER cố xem đơn người khác)

GET  /api/orders?page=0&size=10
     Authorization: Bearer {token}
→ USER: chỉ thấy đơn của mình
→ ADMIN/MANAGER: thấy tất cả

POST /api/orders/{id}/cancel
     Authorization: Bearer {token}
→ 200 | 403 | 409 (không thể hủy)
```

### 2.3 API Gateway Routes (Microservice)

```
# Routing rules
/api/users/**     → http://user-service:8091
/api/products/**  → http://product-service:8092
/api/orders/**    → http://order-service:8093
/api/payments/**  → http://payment-service:8094
/api/files/**     → http://file-service:8096

# File Upload endpoint
POST /api/files/upload
     Authorization: Bearer {token}
     (Multipart Form-Data: file)
→ 201: { fileId, fileUrl, mimeType } | 400 | 403

# Headers Gateway inject sau JWT validation:
X-User-Id:          {UUID từ JWT sub claim}
X-User-Roles:       {ADMIN,USER} (comma-separated)
X-User-Permissions: {PRODUCT_READ,ORDER_CREATE} (comma-separated)
X-User-Email:       {email từ JWT claim}
X-Correlation-Id:   {UUID cho distributed tracing}
```

---

## 3. Business Flows Quan Trọng

### 3.1 Flow Đăng Nhập SSO (Authorization Code + PKCE)

```
1. User mở Monolith App → chưa có session
2. Monolith redirect → SSO /oauth2/authorize?...&code_challenge=H(verifier)
3. SSO hiện login form
4. User nhập credentials → SSO authenticate
   a. Check password hash (BCrypt)
   b. Check account enabled/locked
   c. Check brute-force counter (Redis)
   d. Ghi audit log: LOGIN_SUCCESS hoặc LOGIN_FAILED
5. SSO redirect về Monolith với ?code=ABC&state=...
6. Monolith gọi SSO POST /oauth2/token:
   - code=ABC, code_verifier=verifier (PKCE verify)
   - Nhận: access_token (15 phút), refresh_token (7 ngày), id_token
7. Monolith lưu tokens vào server-side session (Redis)
8. User thấy Dashboard
```

### 3.2 Flow SSO Cross-App (Single Sign-On)

```
1. User đã login Monolith (SSO session tồn tại)
2. User mở Microservice App
3. Microservice App redirect → SSO /oauth2/authorize
4. SSO detect: User đã có session → KHÔNG hỏi password lại
5. SSO redirect về Microservice App với authorization code
6. Microservice App exchange code → nhận tokens
7. User thấy Microservice App Dashboard — no login required!
```

### 3.3 Flow Tạo Order (Microservice)

```
1. User POST /api/orders qua API Gateway với JWT + Idempotency-Key
2. Gateway:
   a. Strip X-User-* headers từ client
   b. Validate JWT (JWKS cache, sig, exp, iss, aud)
   c. Inject trusted headers: X-User-Id, X-User-Roles, X-User-Permissions
   d. Forward đến Order Service :8093
3. Order Service:
   a. CurrentUserResolver đọc X-User-* headers → CurrentUser
   b. authorizationService.requirePermission(user, "ORDER_CREATE")
   c. Check Idempotency-Key trong Redis (SETNX)
   d. Create Order trong @Transactional
   e. Ghi OutboxEvent (ORDER_CREATED) cùng transaction
4. OutboxEventPublisher (Scheduled):
   a. Đọc PENDING outbox events
   b. Publish lên Kafka topic "order-created"
5. Notification Service consume "order-created":
   a. Check event đã xử lý chưa (Idempotent consumer)
   b. Gửi email xác nhận (mock)
6. Order Service response:
   → 201 Created: ApiResponse<OrderResponse>
   → 409 Conflict: Duplicate Idempotency-Key
   → 403 Forbidden: Thiếu ORDER_CREATE permission
```

### 3.4 Flow Phát Hiện Refresh Token Replay Attack

```
1. Attacker đánh cắp RT1 của user
2. Legitimate user dùng RT1 → nhận AT2 + RT2, RT1 bị đánh dấu REVOKED
3. Attacker dùng RT1 (đã bị REVOKE):
   a. SSO tìm RT1 → status = REVOKED
   b. SSO phát hiện REPLAY ATTACK!
   c. SSO revoke TOÀN BỘ token family (RT2, RT3, ...)
   d. User bị force logout
   e. Ghi audit: SECURITY_INCIDENT_REPLAY_ATTACK
4. Legitimate user phải login lại từ đầu
5. Admin nhận notification về security incident
```
