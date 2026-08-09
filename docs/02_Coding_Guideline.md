# SSO Platform - Hướng Dẫn Viết Code (Coding Guideline)

Tài liệu này quy định các tiêu chuẩn lập trình, quy ước đặt tên, cấu trúc package và phong cách viết comment bắt buộc trong toàn bộ dự án **SSO Platform**.

---

## 1. Cấu Trúc Package

### SSO Server

```
com.sso.server
├── config/
│   ├── SecurityConfig.java            # Spring Authorization Server config
│   ├── AuthorizationServerConfig.java # OAuth2 client registration, token settings
│   └── CorsConfig.java
├── controller/
│   ├── AuthController.java            # /auth/* endpoints
│   ├── AdminUserController.java       # /admin/users/*
│   └── AdminRoleController.java       # /admin/roles/*
├── service/
│   ├── UserService.java
│   ├── RoleService.java
│   ├── TokenService.java
│   ├── AuditLogService.java
│   └── BruteForceProtectionService.java
├── repository/
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   └── RefreshTokenRepository.java
├── entity/
│   ├── User.java
│   ├── Role.java
│   ├── Permission.java
│   ├── RefreshToken.java
│   └── AuditLog.java
├── dto/
│   ├── request/
│   └── response/
├── security/
│   ├── CustomUserDetailsService.java
│   ├── JwtCustomizerConfig.java        # Thêm custom claims vào JWT
│   └── SsoUserDetails.java
└── exception/
    ├── GlobalExceptionHandler.java
    └── BusinessException.java
```

### Monolith App

```
com.sso.monolith
├── config/
│   ├── SecurityConfig.java            # @EnableMethodSecurity, OAuth2 Login
│   ├── OAuth2ClientConfig.java        # SSO Client configuration
│   └── AuditConfig.java
├── controller/
│   ├── ProductController.java
│   ├── OrderController.java
│   └── UserProfileController.java
├── service/
│   ├── ProductService.java            # @PreAuthorize ở đây
│   ├── OrderService.java              # @PreAuthorize + resource ownership
│   └── UserProfileService.java
├── security/
│   ├── SsoJwtGrantedAuthoritiesConverter.java  # JWT claims → GrantedAuthority
│   ├── OrderSecurityEvaluator.java             # @orderSecurity bean cho SpEL
│   └── CurrentUserHelper.java                  # Lấy Authentication từ SecurityContext
├── repository/
├── entity/
├── dto/
│   ├── request/
│   └── response/
└── exception/
    └── GlobalExceptionHandler.java
```

### Microservice — mỗi service

```
com.sso.microservice.[service-name]
├── config/
│   └── SecurityConfig.java           # Chỉ CORS, không cần Spring Security full
├── controller/
├── service/
│   └── [Domain]Service.java         # Gọi authorizationService.require(...)
├── security/
│   ├── CurrentUserResolver.java      # Đọc X-User-* headers → CurrentUser
│   └── AuthorizationService.java     # requirePermission, requireOwner
├── repository/
├── entity/
├── dto/
└── exception/
    └── GlobalExceptionHandler.java
```

### API Gateway

```
com.sso.gateway
├── config/
│   ├── GatewayConfig.java            # Routes, filters
│   ├── SecurityConfig.java           # JWKS resource server
│   └── RateLimiterConfig.java
├── filter/
│   ├── JwtValidationFilter.java      # Validate JWT từ JWKS
│   ├── TrustedHeaderInjectionFilter.java  # Inject X-User-* headers
│   └── StripClientHeadersFilter.java      # Strip X-User-* từ client
└── exception/
    └── GatewayErrorHandler.java
```

---

## 2. Quy Tắc Đặt Tên (Naming Conventions)

| Loại | Convention | Ví dụ |
|---|---|---|
| Class | PascalCase | `OrderService`, `CurrentUserResolver` |
| Method | camelCase | `requirePermission`, `getOrderById` |
| Variable | camelCase | `currentUser`, `accessToken` |
| Constant | SCREAMING_SNAKE_CASE | `TOKEN_EXPIRY_SECONDS`, `MAX_LOGIN_ATTEMPTS` |
| Package | lowercase | `com.sso.monolith.security` |
| DB Table | snake_case, plural | `orders`, `refresh_tokens`, `audit_logs` |
| DB Column | snake_case | `user_id`, `created_at`, `is_revoked` |
| Kafka Topic | kebab-case | `order-created`, `payment-completed` |
| DTO suffix | Request/Response | `CreateOrderRequest`, `OrderDetailResponse` |
| Entity suffix | (không suffix) | `Order`, `Product`, `User` |

---

## 3. Tiêu Chuẩn Javadoc Bắt Buộc

### 3.1 Class-level Javadoc

```java
/**
 * Service xử lý logic phân quyền cho các microservices.
 *
 * <p>Tại sao cần class này thay vì dùng @PreAuthorize?
 * Trong kiến trúc microservice, identity của user đến từ trusted headers
 * (X-User-Id, X-User-Roles, X-User-Permissions) do API Gateway inject sau khi validate JWT.
 * Spring Security @PreAuthorize cần SecurityContext được setup từ JWT trực tiếp,
 * nhưng ở đây ta dùng header-based identity → cần AuthorizationService tường minh.
 *
 * <p>Lợi ích:
 * - Code authorization rõ ràng, dễ audit
 * - Không phụ thuộc vào Spring Security filter chain phức tạp
 * - Dễ unit test (chỉ cần mock CurrentUser)
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService { ... }
```

### 3.2 Method-level Javadoc

```java
/**
 * Kiểm tra user có quyền thực hiện thao tác yêu cầu không.
 *
 * <p>Logic kiểm tra: so sánh permission yêu cầu với danh sách permissions
 * của user được inject từ API Gateway vào header X-User-Permissions.
 *
 * <p>Tại sao ném exception thay vì trả về boolean?
 * Throw exception ngay tại đây để ngăn business logic tiếp tục chạy,
 * GlobalExceptionHandler sẽ bắt và trả về HTTP 403 chuẩn hóa cho client.
 *
 * @param currentUser Thông tin user hiện tại (từ Gateway headers)
 * @param permission  Tên permission cần kiểm tra (ví dụ: "ORDER_CANCEL")
 * @throws BusinessException HTTP 403 nếu user không có permission
 */
public void requirePermission(CurrentUser currentUser, String permission) { ... }
```

### 3.3 Inline Comment — phải trả lời "Tại sao?"

```java
// QUAN TRỌNG: Strip X-User-* headers do client gửi lên trước khi validate JWT
// Nếu không làm bước này, attacker có thể tự inject:
//   X-User-Id: 1 (admin user)
//   X-User-Roles: ADMIN
// và bypass toàn bộ authorization của service
exchange.getRequest().mutate()
    .headers(headers -> {
        headers.remove("X-User-Id");
        headers.remove("X-User-Roles");
        headers.remove("X-User-Permissions");
    });
```

---

## 4. Tiêu Chuẩn DTO

```java
// Dùng Java Record cho immutable DTOs
// Tại sao Record? → Immutable by default, concise, built-in equals/hashCode/toString

public record CreateProductRequest(
    @NotBlank(message = "Tên sản phẩm không được trống")
    String name,

    @NotNull(message = "Giá sản phẩm không được null")
    @Positive(message = "Giá phải lớn hơn 0")
    BigDecimal price,

    @NotNull
    @Min(value = 0, message = "Tồn kho không được âm")
    Integer stock
) {}

public record ProductResponse(
    Long id,
    String name,
    BigDecimal price,
    Integer stock,
    String createdBy,
    Instant createdAt
) {}
```

---

## 5. Tiêu Chuẩn Xử Lý Lỗi (Exception Handling)

### 5.1 BusinessException

```java
/**
 * Custom exception chuẩn hóa cho tất cả lỗi nghiệp vụ.
 * Chứa ErrorCode để GlobalExceptionHandler map ra HTTP status đúng.
 */
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
```

### 5.2 ErrorCode Enum

```java
public enum ErrorCode {
    // Authentication errors
    UNAUTHORIZED(401, "Yêu cầu đăng nhập"),
    TOKEN_EXPIRED(401, "Token đã hết hạn"),
    INVALID_TOKEN(401, "Token không hợp lệ"),

    // Authorization errors
    FORBIDDEN(403, "Không có quyền thực hiện"),

    // Business errors
    NOT_FOUND(404, "Không tìm thấy tài nguyên"),
    INVALID_INPUT(400, "Dữ liệu đầu vào không hợp lệ"),
    CONFLICT(409, "Xung đột dữ liệu"),

    // Security
    BRUTE_FORCE_DETECTED(429, "Quá nhiều lần thử đăng nhập"),
    RATE_LIMIT_EXCEEDED(429, "Vượt tần suất gọi API"),

    // System
    INTERNAL_ERROR(500, "Lỗi hệ thống");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
```

---

## 6. Tiêu Chuẩn API Response

```java
/**
 * Wrapper chuẩn hóa cho tất cả API responses.
 * Đảm bảo Frontend luôn nhận cùng một cấu trúc JSON dù thành công hay thất bại.
 */
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    String errorCode,  // null nếu success
    Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Thành công", data, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    public static ApiResponse<Void> error(ErrorCode code, String message) {
        return new ApiResponse<>(false, message, null,
            code.name(), Instant.now());
    }
}
```

---

## 7. Quy Tắc Transaction & Database

```java
// @Transactional luôn đặt ở Service Layer, KHÔNG phải Controller
// Tại sao? → Một use case thường gồm nhiều bước ghi DB
// Nếu bước 2 lỗi, bước 1 phải rollback toàn bộ

@Service
@Transactional(readOnly = true)  // Mặc định read-only cho queries
public class OrderService {

    @Transactional  // Override cho write operations
    public OrderResponse createOrder(CreateOrderRequest req, CurrentUser user) {
        // 1. Validate
        // 2. Check stock
        // 3. Save order
        // 4. Save outbox event (cùng transaction với order)
        // → Atomic: hoặc cả 2 thành công, hoặc cả 2 rollback
    }
}
```

---

## 8. Quy Tắc Audit Log

```java
// Mọi thao tác quan trọng phải được ghi audit log:
// LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT
// TOKEN_REFRESH, TOKEN_REVOKE
// USER_CREATED, USER_DISABLED, PASSWORD_CHANGED
// ROLE_ASSIGNED, ROLE_REMOVED
// PRODUCT_CREATED, PRODUCT_DELETED
// ORDER_CREATED, ORDER_CANCELLED, PAYMENT_REFUNDED

@Aspect
@Component
public class AuditLogAspect {

    /**
     * Intercept tất cả write operations và tự động ghi audit log.
     * Tại sao dùng AOP thay vì gọi auditService.log() thủ công?
     * - Tránh code lặp trong mọi service method
     * - Audit log không thể bị developer quên gọi
     * - Separation of Concerns: business logic tách khỏi audit logic
     */
    @Around("@annotation(Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable { ... }
}
```
