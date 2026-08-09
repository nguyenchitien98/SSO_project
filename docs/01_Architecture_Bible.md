# SSO Platform - Cẩm Nang Kiến Trúc (Architecture Bible)

Tài liệu này định nghĩa chi tiết kiến trúc hệ thống, ranh giới dịch vụ, luồng bảo mật, mô hình dữ liệu và các quyết định thiết kế (Design Decisions) trong **SSO Platform**.

---

## 1. Nguyên Tắc Kiến Trúc (Architecture Principles)

1. **Security by Design:** Mọi service đều phải xác thực danh tính. Không có "internal only" bypass.
2. **Trust Boundary:** Client bên ngoài KHÔNG BAO GIỜ được phép tự inject `X-User-*` headers. Gateway là ranh giới tin cậy duy nhất.
3. **Stateless Services:** Mọi microservice phải stateless. State lưu ở Redis hoặc DB, không lưu trong RAM service.
4. **Separation of Authentication & Authorization:** SSO Server chịu Authentication. Từng App chịu Authorization.
5. **Defense in Depth:** Nhiều lớp bảo vệ: Gateway → Service → Business Logic.
6. **Fail Secure:** Khi không chắc, từ chối request. Không mặc định cho phép.

> [!NOTE]
> **Lưu ý về Identity Provider (IdP):** Sơ đồ kiến trúc hệ thống (`ArchitectureSSO.png`) mô tả **Keycloak** như một giải pháp Identity Provider doanh nghiệp chuẩn hóa. Tuy nhiên, để tối ưu hóa giá trị học tập và viết code Java nâng cao, dự án này tự phát triển module `sso-server` bằng **Spring Authorization Server 1.3**. Về mặt kiến trúc, cả hai đều tuân thủ nghiêm ngặt chuẩn OAuth2/OIDC và JWKS nên có thể thay thế cho nhau mà không ảnh hưởng đến các service phía sau.


---

## 2. Mô Hình Phân Quyền (RBAC + ABAC)

### 2.1 Roles

```
ADMIN      — Toàn quyền hệ thống
MANAGER    — Quản lý sản phẩm, đơn hàng, xem báo cáo
STAFF      — Tạo/sửa sản phẩm, xử lý đơn hàng
AUDITOR    — Chỉ xem (read-only) tất cả tài nguyên
USER       — Xem sản phẩm, tạo/xem đơn hàng CỦA CHÍNH MÌNH
SUPPORT    — Xem đơn hàng, hỗ trợ khách hàng
```

### 2.2 Permissions (Quyền hạn chi tiết)

```
USER_READ        USER_CREATE    USER_UPDATE    USER_DELETE
PRODUCT_READ     PRODUCT_CREATE PRODUCT_UPDATE PRODUCT_DELETE
ORDER_READ       ORDER_CREATE   ORDER_CANCEL   ORDER_REFUND
PAYMENT_READ     PAYMENT_CREATE PAYMENT_REFUND
AUDIT_READ
```

### 2.3 Role → Permission Mapping

| Role | Permissions |
|---|---|
| ADMIN | Tất cả permissions |
| MANAGER | USER_READ, PRODUCT_READ/CREATE/UPDATE, ORDER_READ/CANCEL/REFUND, PAYMENT_READ, AUDIT_READ |
| STAFF | PRODUCT_READ/CREATE/UPDATE, ORDER_READ |
| AUDITOR | *_READ (tất cả read permissions) |
| USER | PRODUCT_READ, ORDER_READ(own), ORDER_CREATE |
| SUPPORT | USER_READ, ORDER_READ, PAYMENT_READ |

### 2.4 Resource Ownership (ABAC)

```
USER chỉ được thao tác trên tài nguyên của CHÍNH MÌNH:
- GET /orders/{id}   → Chỉ được nếu order.userId == currentUser.id
- DELETE /orders/{id} → Chỉ được nếu order.userId == currentUser.id VÀ đơn chưa được confirm

ADMIN/MANAGER có thể thao tác tài nguyên của bất kỳ user nào.
```

---

## 3. Cấu Trúc Repository

```
sso-platform/
│
├── sso-server/                    # Spring Authorization Server (OAuth2/OIDC)
│   ├── src/
│   └── pom.xml
│
├── monolith-app/                  # Spring Boot Monolith với Spring Security
│   ├── src/
│   │   └── main/java/com/sso/monolith/
│   │       ├── config/            # Security, CORS, Bean config
│   │       ├── controller/        # REST Controllers
│   │       ├── service/           # Business Logic + @PreAuthorize
│   │       ├── repository/        # JPA Repositories
│   │       ├── entity/            # JPA Entities
│   │       ├── dto/               # Request/Response DTOs
│   │       ├── security/          # Custom Security components
│   │       ├── audit/             # Audit log AOP
│   │       └── exception/         # Global exception handler
│   └── pom.xml
│
├── microservice-app/              # Microservice system
│   ├── api-gateway/               # Spring Cloud Gateway :8090
│   ├── config-server/             # Spring Cloud Config Server :8888
│   ├── user-service/              # :8091
│   ├── product-service/           # :8092
│   ├── order-service/             # :8093
│   ├── payment-service/           # :8094
│   ├── notification-service/      # :8095
│   ├── file-service/              # File upload & MinIO integration :8096
│   └── common-contracts/          # Shared event DTOs ONLY (không có business logic)
│
├── infrastructure/
│   ├── docker-compose.infra.yml   # PostgreSQL, Redis, Kafka, MinIO, Prometheus, Grafana, Loki
│   └── docker-compose.full.yml    # Full stack including all services
│
└── docs/                          # Tài liệu kiến trúc, sprint plan, ADR

```

---

## 4. Luồng OAuth2 Authorization Code + PKCE

```
1. User truy cập Monolith/Microservice Web → chưa login
2. App redirect → SSO Server /oauth2/authorize?
      client_id=monolith-web
      response_type=code
      redirect_uri=http://monolith/login/oauth2/code/sso
      scope=openid profile email
      code_challenge=...    (PKCE)
      state=...

3. SSO Server hiển thị login form
4. User nhập username/password → SSO authenticate
5. SSO redirect về App với Authorization Code
6. App backend gọi SSO /oauth2/token:
      grant_type=authorization_code
      code=...
      code_verifier=... (PKCE)
      → Nhận: access_token, refresh_token, id_token

7. App lưu token vào secure session (không lưu ở browser localStorage)
8. Mọi API call kèm access_token trong Authorization: Bearer header
```

---

## 5. JWT Structure (Access Token Claims)

```json
{
  "iss": "http://sso-server:9000",
  "sub": "user-uuid-123",
  "aud": ["monolith-api", "microservice-api"],
  "exp": 1786250000,
  "iat": 1786246400,
  "jti": "unique-token-id",
  "scope": ["openid", "profile", "email"],
  "roles": ["USER"],
  "permissions": ["PRODUCT_READ", "ORDER_READ", "ORDER_CREATE"],
  "email": "user@example.com",
  "name": "Nguyễn Văn A"
}
```

**Quy tắc Claims:**
- `sub` = UUID của user (không phải email, không phải auto-increment ID)
- `roles` và `permissions` từ RBAC của SSO Server
- Access token TTL: **15 phút**
- Refresh token TTL: **7 ngày** (với Refresh Token Rotation)

---

## 6. Cơ Chế Phân Quyền Monolith

### 6.1 Flow

```
HTTP Request
    ↓
Spring Security Filter Chain
    ↓
OAuth2LoginAuthenticationFilter (validate ID token từ SSO)
    ↓
SecurityContext (lưu Authentication với GrantedAuthority từ JWT claims)
    ↓
@PreAuthorize tại Service Layer
    ↓
Business Logic
    ↓
Database
```

### 6.2 Cách Spring Security load Authorities

```java
// Monolith tự convert JWT roles + permissions thành GrantedAuthority
// Tại sao làm thế này?
// - Spring @PreAuthorize("hasAuthority('PRODUCT_CREATE')") cần GrantedAuthority
// - Ta map permissions từ JWT claim → SimpleGrantedAuthority
// - Không cần gọi lại SSO Server trên mỗi request → tiết kiệm latency
public class SsoJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        List<String> roles = jwt.getClaimAsStringList("roles");
        // ...
    }
}
```

### 6.3 Sử dụng @PreAuthorize

```java
// Service Layer — đây là nơi enforce authorization, KHÔNG phải Controller
@Service
public class ProductService {

    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ProductResponse getProduct(Long id) { ... }

    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public ProductResponse createProduct(CreateProductRequest req) { ... }

    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    public void deleteProduct(Long id) { ... }
}

// Resource Ownership với SpEL
@Service
public class OrderService {

    @PreAuthorize("hasAuthority('ORDER_READ') and @orderSecurity.isOwnerOrAdmin(authentication, #orderId)")
    public OrderResponse getOrder(Long orderId) { ... }
}
```

---

## 7. Cơ Chế Phân Quyền Microservice

### 7.1 Flow tổng thể

```
Internet
    ↓
API Gateway (Trust Boundary)
    │
    ├── Strip X-User-* headers do client gửi lên (QUAN TRỌNG!)
    ├── Validate JWT: signature, expiration, issuer, audience
    ├── Extract claims: userId, roles, permissions
    └── Inject trusted headers vào request nội bộ:
            X-User-Id: user-uuid-123
            X-User-Roles: USER
            X-User-Permissions: PRODUCT_READ,ORDER_READ,ORDER_CREATE
    ↓
Internal Network (trusted zone)
    ↓
Microservice
    │
    └── Đọc headers → tạo CurrentUser object → AuthorizationService.require(...)
```

### 7.2 CurrentUser Pattern

```java
// Tại sao dùng Record?
// - Immutable: sau khi tạo từ headers không ai sửa được
// - Thread-safe: an toàn trong môi trường concurrent
// - Concise: không cần Lombok @Data
public record CurrentUser(
    String userId,
    String email,
    Set<String> roles,
    Set<String> permissions
) {
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
```

### 7.3 AuthorizationService (thay thế @PreAuthorize)

```java
@Service
public class AuthorizationService {

    /**
     * Kiểm tra user có permission yêu cầu không.
     * Ném ForbiddenException (HTTP 403) nếu không đủ quyền.
     * Tại sao không dùng @PreAuthorize như Monolith?
     * - Microservice không load Spring Security SecurityContext từ JWT trực tiếp
     * - Headers đến từ Gateway (trusted internal source)
     * - Cần explicit authorization check để code rõ ràng và dễ audit
     */
    public void requirePermission(CurrentUser user, String permission) {
        if (!user.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                "Yêu cầu quyền: " + permission);
        }
    }

    public void requireOwnerOrAdmin(CurrentUser user, String resourceUserId) {
        if (!user.userId().equals(resourceUserId) && !user.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                "Chỉ được truy cập tài nguyên của chính mình");
        }
    }
}
```

---

## 8. Service-to-Service Authentication

```
Khi Order Service gọi Payment Service:

Order Service
    ↓ (lấy service access token)
SSO Server: POST /oauth2/token
    grant_type=client_credentials
    client_id=order-service
    client_secret=...
    scope=payment:write
    ↓
Service Access Token (sub = "order-service", không có roles user)
    ↓
Payment Service validate token
    ↓ (chỉ cho phép nếu client = "order-service")
Payment Service xử lý request
```

**Tại sao cần điều này?**
- Tránh một service giả mạo service khác
- Payment Service không nên cho phép bất kỳ ai gọi `/internal/payments`
- Mỗi service có identity riêng, auditable

---

## 9. Database Architecture

### SSO Server DB (`sso_db`)

```sql
users           -- id, username, email, password_hash, enabled, locked
roles           -- id, name (ADMIN, MANAGER, USER...)
permissions     -- id, name (PRODUCT_READ, ORDER_CREATE...)
user_roles      -- user_id, role_id
role_permissions -- role_id, permission_id
oauth_clients   -- client_id, secret, scopes, redirect_uris
sessions        -- id, user_id, ip, user_agent, created_at, expires_at
refresh_tokens  -- id, user_id, token_hash, family_id, revoked, expires_at
audit_logs      -- id, user_id, action, resource, ip, timestamp, metadata
```

### Monolith DB (`monolith_db`)

```sql
user_profiles   -- id (FK=SSO user_id), first_name, last_name, phone, avatar
products        -- id, name, price, stock, created_by, created_at
orders          -- id, user_id, status, total_amount, created_at
order_items     -- id, order_id, product_id, quantity, unit_price
payments        -- id, order_id, amount, method, status, created_at
audit_logs      -- id, actor_id, action, entity, entity_id, changed_at
```

### Microservice DBs (mỗi service có DB riêng)

```sql
-- user_db
user_profiles   -- id (=SSO user_id), display_name, avatar, preferences

-- product_db
products        -- id, name, description, price, stock, category_id
categories      -- id, name, parent_id

-- order_db
orders          -- id, user_id, status, total_amount, idempotency_key
order_items     -- id, order_id, product_id, quantity, unit_price
outbox_events   -- id, event_type, payload, status, created_at

-- payment_db
payments        -- id, order_id, user_id, amount, method, status
outbox_events   -- id, event_type, payload, status, created_at
```

---

## 10. Asymmetric Key Management (RSA)

```
SSO Server:
  - Generate RSA-2048 key pair
  - Sign JWT bằng private key
  - Expose public key qua:
      GET /.well-known/openid-configuration
      GET /oauth2/jwks

API Gateway và Monolith:
  - Fetch JWKS từ SSO Server khi khởi động
  - Cache public keys
  - Verify JWT signature bằng public key
  - Khi key rotation: SSO publish kid mới, các client tự fetch JWKS mới
```

**Tại sao dùng asymmetric key thay vì shared secret?**
- Chỉ SSO Server biết private key → chỉ SSO mới ký được JWT
- Mọi service chỉ cần public key để verify → không cần share secret
- Key rotation an toàn: không cần deploy lại tất cả services khi rotate

---

## 11. Shared & Tích Hợp Layer (Shared Services)

Để hệ thống vận hành trơn tru và tránh lặp lại code ở các service, ta xây dựng các thành phần dùng chung sau:

1. **Đồng bộ User/Role:** Khi User được tạo hoặc cập nhật role tại SSO Server, một event (`user-registered` hoặc `user-updated`) được gửi qua Kafka. Các service như `user-service` sẽ consume để đồng bộ thông tin profile.
2. **Audit Log & Login History:** Một centralized database hoặc system (Loki/ELK) thu thập audit logs từ AOP Aspect của các service và login history từ SSO Server.
3. **Centralized Config Center (Spring Cloud Config Server):** Quản lý cấu hình tập trung cho tất cả microservices, hỗ trợ mã hóa các thông tin nhạy cảm.
4. **Email / Notification Service:** Consume các event thanh toán và đơn hàng từ Kafka để gửi email thông báo cho khách hàng mà không làm block luồng xử lý chính.
5. **Centralized File Storage (MinIO):** Dịch vụ lưu trữ Object Storage dùng chung để lưu trữ avatar người dùng, hình ảnh sản phẩm.

---

## 12. Kiến Trúc Object Storage (MinIO)

Hệ thống sử dụng **MinIO** (tương thích AWS S3 API) làm Object Storage tập trung:

- **Bảo mật:** Client không upload file trực tiếp lên MinIO công khai. Quá trình upload/download được bảo mật thông qua 2 cơ chế:
  - **Internal upload:** Microservices gọi SDK MinIO để upload file (avatar, product image) bằng credentials cấu hình tập trung.
  - **Presigned URL:** Sinh URL có giới hạn thời gian (ví dụ: 15 phút) để client download/view các file riêng tư hoặc upload trực tiếp mà không lộ credentials.
- **Microservice Integration:** `file-service` là gateway duy nhất xử lý file upload từ frontend, thực hiện validate file size, file type (MIME type) trước khi chuyển vào MinIO.

---

## 13. Cơ Chế Xác Thực Hai Lớp (2FA / TOTP)

Hệ thống hỗ trợ xác thực hai lớp (Two-Factor Authentication - 2FA) thông qua thuật toán **TOTP (Time-Based One-Time Password - RFC 6238)**:

1. **Kích hoạt (Setup):**
   - User yêu cầu bật 2FA. SSO Server tạo một ngẫu nhiên TOTP Secret Key (32 ký tự Base32).
   - SSO Server sinh QR Code chứa URL: `otpauth://totp/SSO-Platform:username?secret=KEY&issuer=SSO-Platform`.
   - User quét QR bằng Google Authenticator / Microsoft Authenticator.
   - User nhập mã 6 số để xác nhận bật thành công. Secret được mã hóa AES và lưu vào DB.
2. **Xác thực (Verification):**
   - Khi đăng nhập bằng password đúng, nếu user đã bật 2FA, SSO Server trả về JWT tạm thời hoặc session state yêu cầu nhập mã OTP (HTTP 200 với status `REQUIRES_2FA`).
   - Client hiển thị màn hình nhập mã OTP 6 số.
   - User gửi mã OTP lên `/oauth2/verify-2fa`. SSO Server xác thực bằng thuật toán TOTP.
   - Xác thực thành công → Issue access token + refresh token chính thức.

---

## 14. Kiến Trúc Social Login (Google / Microsoft OAuth2)

Hệ thống hỗ trợ đăng nhập qua bên thứ ba (Federated Identity) sử dụng OAuth2/OIDC:

```
[Browser]          [SSO Server]          [Google/Microsoft Identity]
    │                   │                             │
    │─── Chọn Login ───>│                             │
    │   with Google     │                             │
    │                   │────── Redirect to Google ──>│
    │<── Nhập tài khoản ──────────────────────────────│
    │─── Authenticate ───────────────────────────────>│
    │<── Authorization Code ──────────────────────────│
    │─── Code ─────────>│                             │
    │                   │────── Exchange Code ───────>│
    │                   │<───── ID Token & Profile ───│
    │                   │                             │
    │                   │ (Map email & link account)  │
    │<── Issue Token ───│                             │
```

- **Liên kết tài khoản (Account Linking):** Khi nhận được ID Token từ Google/Microsoft, SSO Server kiểm tra email. Nếu email đã tồn tại trong hệ thống, tự động liên kết (link) tài khoản local với Social ID. Nếu chưa tồn tại, tạo User mới với password ngẫu nhiên được khóa.
- **Bảo mật:** State parameter được gửi để chống CSRF trong luồng Social Login.

