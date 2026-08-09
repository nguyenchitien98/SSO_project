# SSO Platform - Hướng Dẫn Chạy Cho AI (Claude Rules / Gemini Rules)

Bạn là AI coding assistant có năng lực Staff Engineer hỗ trợ phát triển dự án **SSO Platform** — một hệ thống SSO production-grade gồm:

- **sso-server/**: Spring Authorization Server (OAuth2/OIDC)
- **monolith-app/**: Monolith Spring Boot + Spring Security + @PreAuthorize
- **microservice-app/**: API Gateway + 5 microservices với Header-based Authorization
- **frontend/**: Next.js 15 + TypeScript (App Router, React Server Components)

---

## DANH SÁCH TÀI LIỆU BẮT BUỘC ĐỌC THEO NHÓM TASK

| Khi làm task | Đọc các file này |
|---|---|
| Bất kỳ task nào | `00_Project_Vision.md`, `01_Architecture_Bible.md`, `05_Sprint_Plan.md`, `16_ADR.md` |
| Backend Java | `02_Coding_Guideline.md`, `09_Advanced_Senior_Java_Conventions.md`, `17_2FA_TOTP_Guide.md`, `18_MinIO_File_Storage.md` |
| Frontend UI | `08_UI_UX_TypeScript_Standard.md`, `11_Code_Quality_Linting_Formatting.md` |
| Database | `04_Database_Schema.md`, `10_Database_Migration_Flyway_Standard.md` |
| Security | `07_Security_Model.md`, `17_2FA_TOTP_Guide.md` |
| Logging/Infra / CI-CD | `12_Structured_Logging_Standard.md`, `13_Infrastructure_Docker_Orchestration.md`, `19_CI_CD_Pipeline.md` |
| API design & flows | `14_Backlog_Business_Flows.md`, `15_Sequence_Diagrams.md` |

---

## QUY TRÌNH BẮT BUỘC KHI NHẬN TASK

### Bước 1: Đọc Tài Liệu (luôn luôn làm trước)

```
1. docs/00_Project_Vision.md      — Phạm vi và mục tiêu
2. docs/01_Architecture_Bible.md  — Kiến trúc, security model, RBAC table
3. docs/02_Coding_Guideline.md    — Package structure, Javadoc, naming
4. docs/05_Sprint_Plan.md         — Sprint đang làm
```

### Bước 2: Xác Nhận Scope

- Nói rõ: task này thuộc module nào, package nào
- Liệt kê file sẽ tạo mới / sửa đổi
- Xác nhận giới hạn: không tự ý import thư viện ngoài pom.xml

### Bước 3: Implement

- Code hoàn chỉnh, không placeholder
- Javadoc tiếng Việt cho mọi class và method public
- Comment giải thích "Tại sao" cho logic security, transaction, async

### Bước 4: Test

- Unit Test với Mockito (ít nhất happy path + 2 edge cases)
- Integration Test với Testcontainers nếu cần DB/Redis/Kafka

---

## QUYẾT ĐỊNH KIẾN TRÚC QUAN TRỌNG (KHÔNG ĐƯỢC THAY ĐỔI)

### Authorization Pattern

| Nơi | Cách phân quyền | Ví dụ |
|---|---|---|
| **Monolith Service Layer** | `@PreAuthorize("hasAuthority('PRODUCT_CREATE')")` | Đặt ở Service, không phải Controller |
| **Microservice** | `authorizationService.requirePermission(currentUser, "PRODUCT_CREATE")` | Explicit call |
| **API Gateway** | Spring Cloud Gateway + JWKS JWT validation | Strip X-User-* headers trước |

### Security Rules

```
✅ PHẢI strip X-User-* headers từ client TRƯỚC khi validate JWT ở Gateway
✅ PHẢI validate JWT: signature (JWKS), expiration, issuer, audience
✅ PHẢI dùng RSA asymmetric key (không shared secret)
✅ PHẢI lưu Refresh Token hash (không plain text)
✅ PHẢI implement Refresh Token Rotation
✅ PHẢI ghi Audit Log cho mọi security event
✅ @PreAuthorize đặt ở SERVICE LAYER, không phải Controller
```

### Package Structure (tuân thủ tuyệt đối)

**SSO Server:**
```
com.sso.server.{config,controller,service,repository,entity,dto,security,exception}
```

**Monolith:**
```
com.sso.monolith.{config,controller,service,repository,entity,dto,security,audit,exception}
```

**Microservice (mỗi service):**
```
com.sso.microservice.[service].{config,controller,service,repository,entity,dto,security,exception}
```

**API Gateway:**
```
com.sso.gateway.{config,filter,exception}
```

---

## TIÊU CHUẨN JAVADOC BẮT BUỘC

### Class-level:
```java
/**
 * [Mô tả vai trò nghiệp vụ của class].
 *
 * <p>Tại sao sử dụng [annotation/pattern này]?
 * [Giải thích lý do thiết kế, so sánh với phương án khác]
 *
 * <p>Sprint: [số sprint]
 * <p>ADR: [số ADR nếu có]
 */
```

### Method-level:
```java
/**
 * [Mô tả chức năng method].
 *
 * <p>Tại sao [logic đặc biệt này]?
 * [Giải thích security rationale hoặc performance trade-off]
 *
 * @param [tên param] [mô tả ý nghĩa]
 * @return [mô tả kết quả trả về]
 * @throws BusinessException khi [điều kiện gây lỗi]
 */
```

---

## CẤM TUYỆT ĐỐI

```
❌ Code giả: // TODO, return null, UnsupportedOperationException
❌ Hardcode secret/password trong source code
❌ Dùng shared secret (symmetric key) cho JWT
❌ Để client inject X-User-* headers
❌ Gọi Auth Service trên mỗi request để validate JWT
❌ @PreAuthorize ở Controller Layer
❌ Dùng chung database giữa các microservices
❌ Thiếu Javadoc tiếng Việt
❌ Publish Kafka trực tiếp trong @Transactional (phải dùng Outbox Pattern)
```

---

## QUY TẮC FRONTEND TYPESCRIPT (BẮT BUỘC)

```
✅ Luôn khai báo type cụ thể cho mọi biến, tham số, return value
✅ Dùng interface cho object shapes, type cho unions/primitives
✅ Dùng unknown thay vì any cho dữ liệu chưa biết → validate rồi mới dùng
✅ Dùng type guard functions để narrow types (không dùng as bừa bãi)
✅ Mọi component phải có interface Props riêng
✅ Dùng import type { Foo } (không phải import { Foo }) cho type-only imports
✅ Server Component là default — thêm 'use client' chỉ khi thực sự cần
✅ CSS Modules cho mọi styles — không inline style, không Tailwind
✅ Dùng CSS Variables (design tokens) trong mọi CSS Module

❌ any — ESLint báo lỗi error, commit bị chặn
❌ as any hoặc @ts-ignore không có giải thích
❌ console.log trong production code (chỉ dùng logger từ @/lib/logger)
❌ Inline styles (style={{ color: 'red' }})
❌ var (chỉ dùng const và let)
❌ Function thiếu return type annotation
❌ Props destructure không có type
```

## TECH STACK FRONTEND

```
Framework:    Next.js 15 (App Router)
Language:     TypeScript 5 (strict mode)
Styling:      CSS Modules + CSS Variables
State:        React useState/useReducer (local), không dùng Redux
Data fetch:   Server Components (RSC) + Typed fetch wrapper (src/lib/api/client.ts)
Auth:         NextAuth.js v5 (SSO OAuth2 client)
i18n:         next-intl (VI + EN)
Linting:      ESLint strict + @typescript-eslint/strict-type-checked
Formatting:   Prettier
```

---

## THÔNG TIN KỸ THUẬT THAM KHẢO NHANH

### Ports
```
SSO Server:           9000
Monolith App:         8080
API Gateway:          8090
Eureka Server:        8761
User Service:         8091
Product Service:      8092
Order Service:        8093
Payment Service:      8094
Notification Service: 8095
PostgreSQL:           5432
Redis:                6379
Kafka:                9092
```

### Default Accounts
```
admin / admin123      → ADMIN role
manager1 / Test@1234  → MANAGER role
staff1 / Test@1234    → STAFF role
auditor1 / Test@1234  → AUDITOR role
support1 / Test@1234  → SUPPORT role
user1 / Test@1234     → USER role
```

### JWT Claims Structure
```json
{
  "sub": "UUID của user",
  "roles": ["USER"],
  "permissions": ["PRODUCT_READ", "ORDER_CREATE"],
  "email": "user@example.com",
  "iss": "http://localhost:9000",
  "aud": ["monolith-api"],
  "exp": 1786250000
}
```

### Kafka Topics
```
order-created          (3 partitions)
payment-completed      (3 partitions)
payment-failed         (3 partitions)
order-status-changed   (3 partitions)
user-registered        (3 partitions)
```

### RBAC Quick Reference
```
ADMIN    → ALL permissions
MANAGER  → USER_READ, PRODUCT_*, ORDER_READ/CANCEL/REFUND, PAYMENT_READ/REFUND, AUDIT_READ
STAFF    → PRODUCT_READ/CREATE/UPDATE, ORDER_READ
AUDITOR  → *_READ only
USER     → PRODUCT_READ, ORDER_READ(own), ORDER_CREATE
SUPPORT  → USER_READ, ORDER_READ, PAYMENT_READ
```
