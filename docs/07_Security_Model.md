# SSO Platform - Security Model Deep Dive

Tài liệu này giải thích chi tiết các quyết định thiết kế bảo mật của hệ thống SSO Platform, bao gồm các câu hỏi phỏng vấn liên quan.

---

## 1. Tại Sao Không Tự Viết Authentication Protocol?

**Vấn đề:** Nhiều dev tự implement login với `username/password → JWT` mà bỏ qua OAuth2/OIDC.

**Rủi ro:**
- Token không có scope → bất kỳ service nào cũng có thể dùng token để call bất kỳ service nào
- Không có chuẩn discovery (`.well-known/openid-configuration`) → services không tự biết cách validate
- Không có key rotation → khi secret bị lộ, phải deploy lại tất cả services
- Thiếu SSO: mỗi app phải tự quản lý login

**Giải pháp:** Dùng **Spring Authorization Server** (chuẩn RFC 6749 / OpenID Connect Core 1.0):
- Chuẩn giao thức được kiểm chứng về security
- PKCE ngăn authorization code interception
- Scope kiểm soát chính xác access của từng token
- JWKS endpoint cho phép key rotation không downtime

---

## 2. Tại Sao Gateway Validate JWT Thay Vì Auth Service?

### Anti-pattern (Sai):

```
Request → Auth Service (decode JWT) → Forward headers → Microservice
```

**Vấn đề:**
- 1000 req/s → 1000 calls đến Auth Service → bottleneck
- Auth Service trở thành Single Point of Failure
- Latency tăng mỗi request (thêm 1 network hop)

### Correct Pattern:

```
Request → API Gateway (validate JWT locally from JWKS cache) → Inject headers → Microservice
```

**Tại sao safe:**
- JWT validation chỉ cần public key → không cần gọi Auth Service
- Public key được cache tại Gateway sau lần fetch đầu tiên
- Khi key rotate, Gateway tự fetch JWKS mới (khi nhận `kid` không biết)
- Latency gần như không tăng (chỉ crypto operation CPU-bound)

---

## 3. Tại Sao Phải Strip X-User-* Headers Từ Client?

### Attack Scenario:

```
Attacker gửi:
POST /api/orders HTTP/1.1
Authorization: Bearer valid_user_token
X-User-Id: 1            ← Admin user ID
X-User-Roles: ADMIN     ← Fake role
```

**Nếu không strip:** Microservice đọc header `X-User-Roles: ADMIN` → nghĩ user là ADMIN → bypass authorization!

### Defense:

```java
// API Gateway filter - chạy TRƯỚC JWT validation
exchange.getRequest().mutate()
    .headers(headers -> {
        // Strip tất cả X-User-* headers từ client
        headers.remove("X-User-Id");
        headers.remove("X-User-Roles");
        headers.remove("X-User-Permissions");
        headers.remove("X-User-Email");
    });
// Sau khi validate JWT → inject lại trusted headers từ JWT claims
```

---

## 4. Monolith vs Microservice: Authorization Khác Nhau Thế Nào?

### Monolith:

```
Spring Security loads JWT → builds Authentication object
→ SecurityContextHolder stores Authentication
→ @PreAuthorize("hasAuthority('PRODUCT_CREATE')") reads from SecurityContext
→ Spring evaluates SpEL expression
→ Allow or throw AccessDeniedException
```

**Ưu điểm:**
- Framework handle hoàn toàn, ít code viết tay
- SpEL expressions mạnh mẽ, flexible
- Method Security đảm bảo authorization ngay tại business logic

**Nhược điểm:**
- Cần Spring Security filter chain phức tạp
- Khó trace authorization decision trong distributed context

### Microservice:

```
Gateway validates JWT → inject X-User-* headers
→ Service receives trusted headers
→ CurrentUserResolver reads headers → builds CurrentUser object
→ Service calls authorizationService.requirePermission(user, "PRODUCT_CREATE")
→ AuthorizationService throws ForbiddenException if denied
```

**Ưu điểm:**
- Explicit, dễ đọc — dev biết rõ cần quyền gì
- Không phụ thuộc Spring Security context
- Dễ unit test (mock CurrentUser, không cần security config)

**Nhược điểm:**
- Phải viết manual check thay vì dùng annotation
- Dễ bị developer quên gọi check

---

## 5. Refresh Token Rotation & Replay Attack Detection

### Vấn đề với Refresh Token đơn giản:

```
RT (refresh token) bị đánh cắp
→ Attacker dùng RT để lấy access token mới vô hạn lần
→ Legitimate user không biết account bị chiếm
```

### Giải pháp: Refresh Token Rotation

```
Lần 1: User có RT1
        → Gọi /oauth2/token với RT1
        → Nhận: AT2 (new access token) + RT2 (new refresh token)
        → RT1 bị đánh dấu REVOKED

Lần 2 (legitimate user): Dùng RT2 → Nhận AT3 + RT3, RT2 REVOKED

Lần 2 (attacker dùng RT1 cũ):
        → Server thấy RT1 đã REVOKED
        → Detect replay attack!
        → Revoke TOÀN BỘ token family của user này
        → User bị force logout → phải login lại
        → Security incident được log
```

### Implementation:

```sql
-- Mỗi refresh token có family_id liên kết với session
refresh_tokens:
  id, token_hash, user_id, family_id, revoked, revoke_reason

-- Khi phát hiện replay (RT đã revoke được dùng lại):
UPDATE refresh_tokens
SET revoked = TRUE, revoke_reason = 'REPLAY_DETECTED'
WHERE family_id = :compromised_family_id;

INSERT INTO audit_logs (action, user_id, details)
VALUES ('SECURITY_INCIDENT_REPLAY_ATTACK', :user_id, ...);
```

---

## 6. Asymmetric Key vs Shared Secret

### Shared Secret (HS256):

```
SSO Server ký JWT bằng secret_key
→ Tất cả services verify bằng cùng secret_key
→ Mọi service biết secret_key
→ Một service bị compromise → tất cả JWTs bị giả mạo được
```

### Asymmetric Key (RS256/RS512):

```
SSO Server: có private_key (bí mật)
           → ký JWT
Gateway/Monolith: có public_key (public)
                 → verify JWT

Chỉ SSO Server mới có thể tạo valid JWT
Services không thể giả mạo JWT ngay cả khi bị hack
```

### Key Rotation:

```
Hiện tại: private_key_v1 ký JWT, public_key_v1 ở JWKS

Rotation:
1. Generate private_key_v2
2. JWKS expose cả public_key_v1 VÀ public_key_v2
3. New tokens ký bằng private_key_v2 (kid: v2)
4. Old tokens (kid: v1) vẫn verify được bằng public_key_v1
5. Sau 15 phút (access token TTL), tất cả v1 tokens expire
6. Remove public_key_v1 khỏi JWKS
```

---

## 7. Service-to-Service vs User Authentication

### Phân biệt 2 loại token:

**User Token:**
```json
{
  "sub": "user-uuid-123",
  "roles": ["USER"],
  "permissions": ["ORDER_CREATE"],
  "email": "user@example.com",
  "iss": "http://sso:9000",
  "aud": ["microservice-api"]
}
```

**Service Token (Client Credentials):**
```json
{
  "sub": "order-service",          ← Subject là service, không phải user
  "client_id": "order-service",
  "scope": "payment:write",         ← Scope giới hạn những gì service được làm
  "iss": "http://sso:9000",
  "aud": ["payment-service"]        ← Chỉ Payment Service mới accept token này
}
```

**Tại sao phân biệt quan trọng:**
- Payment Service chỉ chấp nhận requests từ `order-service` (validated by `sub` claim)
- Không thể dùng User Token để call internal endpoints của Payment Service
- Auditing: phân biệt rõ action do user hay system thực hiện

---

## 8. CSRF vs JWT Bearer Token

### CSRF Attack:

```
User đang login trên bank.com (có session cookie)
Attacker tạo evil.com với form:
  <form action="http://bank.com/transfer" method="POST">
    <input name="amount" value="1000000">
  </form>
  <script>document.forms[0].submit()</script>

Browser tự động gửi session cookie → bank.com nghĩ request hợp lệ!
```

### Tại Sao JWT Bearer Token Không Cần CSRF Protection:

```
JWT trong Authorization: Bearer header
→ Browser KHÔNG tự động gửi header khi submit form từ site khác
→ CSRF attack cần browser tự gửi credentials
→ Bearer token không phải cookie → browser không tự gửi
→ CSRF không work với Bearer token authentication
```

**Nhưng nếu lưu JWT trong Cookie:** Phải enable CSRF protection!

---

## 9. Brute Force Protection

### Strategy:

```
login:attempt:{username} → Redis key
login:attempt:{ip}       → Redis key

Mỗi lần login thất bại:
  INCR login:attempt:{username}  → TTL 5 phút
  INCR login:attempt:{ip}        → TTL 5 phút

Ngưỡng:
  Username: 5 failures → lock tạm 30 phút
  Username: 10 failures → lock vĩnh viễn (cần Admin unlock)
  IP: 20 failures → rate limit IP đó

Khi lock → ghi audit log LOGIN_ACCOUNT_LOCKED
```

### Tại sao Redis thay vì DB?

- Redis INCR là atomic → không race condition
- TTL tự động reset counter sau thời gian → không cần cleanup job
- Tốc độ nhanh hơn nhiều lần so với DB query mỗi login attempt

---

## 10. Câu Hỏi Phỏng Vấn Thường Gặp

**Q: Sự khác biệt giữa Authentication và Authorization?**

A: Authentication = "Bạn là ai?" (verify identity). Authorization = "Bạn được làm gì?" (verify permissions). SSO Server làm Authentication. Monolith/Microservice làm Authorization.

**Q: Tại sao không gọi Auth Service trên mỗi request để validate JWT?**

A: Gây bottleneck O(n) với Auth Service. JWT chứa signature → có thể verify offline bằng public key từ JWKS. Chỉ fetch JWKS một lần và cache.

**Q: Tại sao @PreAuthorize đặt ở Service Layer, không phải Controller?**

A: Controller là HTTP adapter layer — có thể có nhiều entry points khác (Kafka consumer, scheduled job). Đặt authorization ở Service đảm bảo bảo vệ business logic dù vào từ đâu.

**Q: Làm sao phát hiện refresh token bị đánh cắp?**

A: Refresh Token Rotation + Family tracking. Khi RT cũ (đã bị revoke sau lần dùng) bị dùng lại → detect replay → revoke toàn bộ family → force logout user.

**Q: Microservice có nên validate JWT không, hay tin hoàn toàn Gateway?**

A: Trade-off. Tin Gateway (Mode A) → đơn giản, hiệu năng cao. Tự validate JWT (Mode B) → defense in depth, đề phòng Gateway bị bypass. Dự án implement cả hai mode với config flag để switch.

**Q: Tại sao phải mã hóa TOTP Secret trong Database?**

A: Nếu database bị lộ (SQL Injection hoặc rò rỉ backup), kẻ tấn công sẽ có được TOTP secret của tất cả người dùng và tự sinh ra mã OTP, vô hiệu hóa hoàn toàn lớp bảo vệ 2FA. Sử dụng thuật toán mã hóa đối xứng như AES-256 để bảo vệ secret key này trong DB.

**Q: Làm thế nào để đảm bảo an toàn cho File Upload lên MinIO?**

A: 1) Không để client upload trực tiếp mà phải đi qua `file-service` để kiểm tra kích thước (tối đa 5MB) và kiểm tra định dạng thực tế (MIME Type) thay vì tin vào đuôi file. 2) Lưu trữ file bằng tên sinh ngẫu nhiên (UUID) để tránh tấn công Path Traversal hoặc ghi đè file hệ thống. 3) Sử dụng Presigned URL cho các tài liệu nhạy cảm để kiểm soát quyền truy cập có thời hạn.

**Q: Làm thế nào để chống tấn công CSRF trong luồng Social Login?**

A: Sử dụng tham số `state` (một giá trị ngẫu nhiên cryptographically secure) gửi sang Google/Microsoft OAuth2. Khi redirect về, SSO Server phải so khớp `state` này với giá trị được lưu ở Session của User. Nếu không khớp → từ chối request.

---

## 11. Mô Hình Bảo Mật 2FA / TOTP

- **Mã hóa Secret Key:** Cột `totp_secret` trong bảng `users` phải được mã hóa bằng thuật toán đối xứng **AES-256-GCM** với một Key Encryption Key (KEK) lưu trữ an toàn trong biến môi trường hoặc hệ thống quản lý Vault.
- **Chống Replay Attack OTP:** Một mã OTP 6 số chỉ được phép sử dụng một lần duy nhất trong chu kỳ 30 giây. SSO Server sử dụng Redis để lưu cache các mã OTP đã xác thực thành công trong vòng 30 giây. Nếu nhận được cùng một mã OTP lần nữa → Reject ngay lập tức.
- **Backup Codes:** Cung cấp 8 mã backup ngẫu nhiên (8 ký tự) để người dùng đăng nhập trong trường hợp mất thiết bị Authenticator. Các mã này được hash bằng bcrypt trước khi lưu DB.

---

## 12. Bảo Mật Social Login (OAuth2 / OIDC Integration)

- **State Parameter:** Sử dụng để liên kết request authorize ban đầu với callback redirect nhằm ngăn chặn CSRF.
- **Account Linking Safety:** Khi người dùng chọn đăng nhập qua Google, SSO Server nhận email từ ID Token. Trước khi tự động link với local account, hệ thống phải xác nhận email đó đã được Google xác thực (`email_verified` claim == true).
- **SSL / HTTPS Enforcement:** Mọi redirect URIs đăng ký với Google/Microsoft bắt buộc phải sử dụng HTTPS ở môi trường Production.

---

## 13. Bảo Mật File Storage (MinIO)

- **Principle of Least Privilege:** Bucket chứa avatar được chia thành 2 loại:
  - `public-bucket`: Chỉ cho phép Read-Only công khai cho mọi người để xem avatar hoặc product image. Quy trình ghi (Write) chỉ được thực hiện bởi service account của `file-service` thông qua API Gateway.
  - `private-bucket`: Cấm truy cập công khai. Mọi thao tác tải file nhạy cảm bắt buộc phải qua `file-service` để verify token và sinh Presigned URL với thời hạn ngắn (ví dụ: 15 phút).
- **MIME Type Validation:** Sử dụng thư viện Apache Tika hoặc file signature check (magic bytes) để kiểm tra định dạng thực tế của file, ngăn chặn attacker thay đổi đuôi file `.exe`/`.sh` thành `.jpg` để upload mã độc.
- **Path Traversal Prevention:** Loại bỏ tất cả ký tự lạ như `../`, `..\\` trong tên file. Khuyên dùng UUID làm tên file lưu trữ trên MinIO.

