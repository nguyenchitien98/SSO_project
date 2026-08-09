# Quy tắc phát triển dự án SSO Platform (Gemini Rules)

Bạn là một AI Coding Assistant có năng lực Staff Engineer hỗ trợ phát triển dự án **SSO Platform**. Dự án bao gồm sso-server (Spring Authorization Server), monolith-app (Spring Boot với `@PreAuthorize`), và microservice-app (Gateway + 6 microservices).

---

## 1. QUY TẮC BẮT BUỘC (Mandatory Rules)

- **Javadoc tiếng Việt bắt buộc:** Tất cả class public, method public và logic nghiệp vụ phức tạp bắt buộc phải có Javadoc và comment giải thích bằng tiếng Việt chi tiết.
- **Không viết code giả (No Placeholder Code):** Không dùng `// TODO`, `return null`, `throw new UnsupportedOperationException()` hay bất kỳ code giả dạng mockup nào. Code sinh ra phải hoàn chỉnh và chạy được.
- **Cấm chia sẻ Database giữa các Microservices:** Mỗi service dùng database riêng biệt (`user_db`, `product_db`, `order_db`...). Cấm gọi trực tiếp đến DB của service khác.
- **Không dùng chung entity:** Các class JPA Entity không được chia sẻ giữa các microservices hoặc giữa monolith và microservices.
- **Cấm lưu plain-text Secret/Password:** Password của user dùng BCrypt, OAuth2 client secrets lưu bằng Bcrypt hoặc quản lý qua Config Server biến môi trường.

---

## 2. KIẾN TRÚC & PHÂN QUYỀN (Architecture & Security Model)

- **API Gateway là Trust Boundary:** Gateway chịu trách nhiệm validate JWT thông qua JWKS endpoint (`:9000/oauth2/jwks`) và cache public keys.
- **Header Propagation:** Gateway bắt buộc phải loại bỏ các header `X-User-*` do client gửi lên trước khi validate JWT, sau đó mới tự động inject các header tin cậy:
  - `X-User-Id` (UUID)
  - `X-User-Roles` (ADMIN, USER...)
  - `X-User-Permissions` (PRODUCT_CREATE, ORDER_CREATE...)
  - `X-User-Email`
  - `X-Correlation-Id`
- **Hai cơ chế phân quyền khác biệt:**
  - **Monolith App:** Dùng `@PreAuthorize` và SpEL expression (`@PreAuthorize("hasAuthority('ORDER_READ') and @orderSecurity.isOwner(authentication, #id)")`) ở Service Layer để kiểm tra quyền và sở hữu tài nguyên.
  - **Microservice App:** Đọc headers từ Gateway map vào `CurrentUser` ở Controller hoặc Service. Sử dụng class `AuthorizationService` thủ công để kiểm tra quyền tường minh (ví dụ: `authorizationService.requirePermission(currentUser, "PRODUCT_CREATE")`).

---

## 3. CHUẨN CODE & THƯ MỤC (Code Standard & Directory Structure)

- **Java Standard:** Java 21 (Virtual Threads), Spring Boot 3.3.x, Spring Security 6.
- **Package structure:** Theo chuẩn Domain-Driven Design (DDD) thu nhỏ:
  - `config/` (Cấu hình bảo mật, bean)
  - `controller/` (API endpoint, validation)
  - `service/` (Nghiệp vụ và phân quyền)
  - `repository/` (JPA repository)
  - `model/` / `entity/` (JPA Entity)
  - `dto/` (Request/Response DTOs)
- **Lombok:** Được phép dùng `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@Slf4j`. Không lạm dụng `@Data` trên JPA Entity để tránh loop.
- **Spotless:** Chạy `mvn spotless:apply` trước khi commit code.
