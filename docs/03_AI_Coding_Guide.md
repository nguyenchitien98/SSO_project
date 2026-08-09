# SSO Platform - Prompt Chuẩn Cho AI Coding

Tài liệu này chứa các **prompt mẫu ngắn gọn** để dán vào bất kỳ AI Agent nào (Claude, Gemini, Cursor, ChatGPT) để code đúng kiến trúc và tiêu chuẩn của dự án **SSO Platform**.

---

## 🚀 Prompt Khởi Đầu Phiên Làm Việc (Paste Đầu Tiên)

```
Bạn là AI coding assistant có năng lực Staff Engineer hỗ trợ tôi phát triển dự án SSO Platform.

Trước khi code, bắt buộc đọc các tài liệu sau theo thứ tự:
1. docs/00_Project_Vision.md     — Mục tiêu và phạm vi dự án
2. docs/01_Architecture_Bible.md — Kiến trúc, security model, RBAC
3. docs/02_Coding_Guideline.md   — Tiêu chuẩn code, Javadoc, package structure
4. docs/05_Sprint_Plan.md        — Sprint hiện tại đang làm gì

Sau khi đọc xong, xác nhận ngắn gọn bằng tiếng Việt:
- Bạn đã hiểu kiến trúc SSO Server / Monolith / Microservice khác nhau thế nào
- Sprint hiện tại đang cần implement gì
- Hỏi: "Chúng ta bắt đầu task nào hôm nay?"
```

---

## 📦 Prompt Templates Theo Nhóm Task

### Prompt 1 — Implement Feature Mới

```
Dự án: SSO Platform
Module: [sso-server / monolith-app / microservice-app/[service-name] / api-gateway]
Sprint: [Số sprint]
Task: [Tên task, ví dụ: "Implement BruteForceProtectionService"]

Yêu cầu:
1. Đọc docs/01_Architecture_Bible.md phần liên quan trước khi code.
2. Viết code hoàn chỉnh theo đúng package structure trong docs/02_Coding_Guideline.md.
3. Mọi class và method public phải có Javadoc tiếng Việt giải thích:
   - Mục đích nghiệp vụ của class/method
   - Tại sao thiết kế như vậy (Architectural Rationale)
   - Liên kết Sprint/ADR nếu có
4. KHÔNG để code giả (// TODO, return null, throw new UnsupportedOperationException).
5. Viết Unit Test với Mockito bao phủ ít nhất 3 scenarios: happy path + 2 edge cases.
6. Nếu cần Redis/DB → dùng Testcontainers cho Integration Test.

Bắt đầu implement.
```

---

### Prompt 2 — Implement Security Feature

```
Dự án: SSO Platform
Task: [Tên security feature, ví dụ: "JWT Validation Filter tại API Gateway"]

Yêu cầu bắt buộc:
1. Đọc docs/01_Architecture_Bible.md phần "7. Cơ Chế Phân Quyền Microservice" trước.
2. Implement theo đúng Trust Boundary model:
   - PHẢI strip X-User-* headers từ client TRƯỚC khi validate JWT
   - PHẢI validate: signature (từ JWKS), expiration, issuer, audience
   - SAU KHI validate → inject trusted X-User-* headers
3. Comment giải thích tại sao từng bước quan trọng về mặt security (bằng tiếng Việt).
4. Viết Security Test: test với tampered JWT, expired JWT, wrong issuer, fake headers.
5. Không hardcode JWT secret — dùng JWKS endpoint của SSO Server.

Chi tiết kỹ thuật:
- SSO JWKS URL: http://sso-server:9000/oauth2/jwks
- Issuer: http://sso-server:9000
- Audience: [monolith-api / microservice-api]
- Headers inject: X-User-Id (sub claim), X-User-Roles, X-User-Permissions, X-User-Email
```

---

### Prompt 3 — Implement @PreAuthorize (Monolith)

```
Dự án: SSO Platform — Monolith App
Task: Implement [ServiceName] với phân quyền @PreAuthorize

Quy tắc BẮT BUỘC:
1. @PreAuthorize đặt tại SERVICE LAYER, KHÔNG phải Controller.
   - Đúng: @PreAuthorize ở ProductService.createProduct()
   - Sai: @PreAuthorize ở ProductController.createProduct()
2. Dùng permission-based (hasAuthority) thay vì role-based (hasRole) khi có thể:
   - Đúng: @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
   - Sai: @PreAuthorize("hasRole('ADMIN')") (chỉ dùng khi không thể dùng permission)
3. Resource Ownership: dùng Spring Bean với SpEL:
   - @PreAuthorize("@orderSecurity.isOwnerOrAdmin(authentication, #orderId)")
   - Tạo @Component("orderSecurity") với method isOwnerOrAdmin()
4. SecurityConfig phải có @EnableMethodSecurity.
5. Javadoc giải thích tại sao dùng @PreAuthorize, không phải if-else check trong code.

Permissions mapping (từ docs/01_Architecture_Bible.md):
[dán bảng RBAC từ Architecture Bible vào đây]
```

---

### Prompt 4 — Implement AuthorizationService (Microservice)

```
Dự án: SSO Platform — Microservice App — [Service Name]
Task: Implement service logic với AuthorizationService pattern

Quy tắc BẮT BUỘC:
1. KHÔNG dùng @PreAuthorize trong microservice — dùng explicit AuthorizationService calls.
2. Tạo CurrentUserResolver đọc từ X-User-* headers (KHÔNG parse JWT lại).
3. Pattern chuẩn:
   public ResponseEntity<...> someMethod(...) {
       CurrentUser currentUser = currentUserResolver.resolve(request);
       authorizationService.requirePermission(currentUser, "PERMISSION_NAME");
       // business logic
   }
4. Javadoc giải thích tại sao dùng header-based authorization thay vì @PreAuthorize.
5. Viết Unit Test với mock CurrentUser (không cần Spring Security context).

Headers available:
- X-User-Id (String UUID)
- X-User-Roles (comma-separated, ví dụ: "USER,MANAGER")
- X-User-Permissions (comma-separated, ví dụ: "ORDER_READ,ORDER_CREATE")
- X-User-Email (String)
```

---

### Prompt 5 — Implement OAuth2 / SSO Server Feature

```
Dự án: SSO Platform — SSO Server (Spring Authorization Server)
Task: [Tên task, ví dụ: "Implement Refresh Token Rotation"]

Yêu cầu:
1. Dùng Spring Authorization Server 1.3.x API — không tự implement OAuth2 protocol.
2. Javadoc giải thích flow OAuth2 liên quan bằng tiếng Việt.
3. Nếu liên quan đến JWT: dùng asymmetric RSA key, KHÔNG shared secret.
4. Nếu liên quan đến session: persist vào Redis (không in-memory).
5. Ghi Audit Log cho mọi security event: LOGIN, LOGOUT, TOKEN_REFRESH, v.v.
6. Test: Integration Test với real Redis và PostgreSQL (Testcontainers).

Security model tham khảo: docs/01_Architecture_Bible.md
```

---

### Prompt 6 — Implement Kafka Event & Outbox Pattern

```
Dự án: SSO Platform — [Service Name]
Task: Implement [EventName] với Transactional Outbox Pattern

Yêu cầu:
1. KHÔNG publish Kafka trực tiếp trong @Transactional method.
2. Thực hiện Outbox Pattern:
   a. Ghi outbox_event vào DB CÙNG transaction với entity chính
   b. Scheduled Job @Scheduled(fixedDelay=5000) đọc outbox, publish Kafka
   c. Sau publish thành công → update outbox status = SENT
3. Consumer phải Idempotent:
   - Check processed_event_ids (Redis hoặc DB) trước khi xử lý
   - Nếu đã xử lý → skip, không throw error
4. Cấu hình Dead Letter Topic cho consumer failures.
5. Javadoc giải thích tại sao Outbox Pattern và Idempotent Consumer quan trọng.

Kafka topics liên quan: [liệt kê topics]
Event DTO: dùng class trong common-contracts module.
```

---

### Prompt 7 — Viết Security/Integration Test

```
Dự án: SSO Platform
Task: Viết [Security Test / Integration Test] cho [Feature/Service]

Yêu cầu:
1. Dùng Testcontainers (PostgreSQL + Redis) cho Integration Test.
2. Test phải có đầy đủ scenarios:
   - Happy path: request hợp lệ → response đúng
   - Auth failure: JWT invalid/expired → 401
   - Authorization failure: thiếu permission → 403
   - Ownership violation: truy cập tài nguyên của người khác → 403
   - Security attack: tampered JWT, fake headers → bị reject
3. Dùng @SpringBootTest(webEnvironment = RANDOM_PORT).
4. Không hardcode port.
5. Comment tiếng Việt giải thích mục đích từng test case.

Security attacks cần test (áp dụng nếu là gateway/auth test):
- JWT với alg:none
- JWT với wrong issuer
- JWT với expired exp
- Tampered payload
- Fake X-User-Id header
- Brute force login
```

---

### Prompt 8 — Fix Bug / Debug

```
Dự án: SSO Platform
Service: [Tên service]
Lỗi: [Mô tả lỗi hoặc stacktrace]

Thực hiện:
1. RCA (Root Cause Analysis): xác định nguyên nhân gốc rễ.
2. Fix lỗi mà KHÔNG vi phạm kiến trúc trong docs/01_Architecture_Bible.md.
3. Viết comment giải thích tại sao lỗi xảy ra và cách phòng ngừa.
4. Thêm Unit Test tái hiện lỗi (test case phải FAIL trước khi fix, PASS sau khi fix).
5. Kiểm tra fix không phá vỡ test hiện có.
```

---

## ⚠️ Quy Tắc Tuyệt Đối Không Vi Phạm

```
❌ KHÔNG để code giả: // TODO, return null, throw new UnsupportedOperationException()
❌ KHÔNG hardcode secrets, passwords, JWT keys trong source code
❌ KHÔNG dùng shared secret cho JWT — phải dùng RSA asymmetric key
❌ KHÔNG để client inject X-User-* headers bypass Gateway
❌ KHÔNG gọi Auth Service trên mỗi request để validate JWT (dùng JWKS + local validation)
❌ KHÔNG publish Kafka trực tiếp trong @Transactional — phải dùng Outbox Pattern
❌ KHÔNG đặt @PreAuthorize ở Controller (đặt ở Service Layer)
❌ KHÔNG share database giữa các microservices
❌ KHÔNG bỏ qua Javadoc và comment tiếng Việt

✅ Mọi class public → Javadoc tiếng Việt
✅ Mọi method public → Javadoc với @param, @return, @throws
✅ Logic security → Comment giải thích tại sao (threat model)
✅ Sau mỗi Sprint → System phải compile và pass test
```
