# SSO Platform - Tiêu Chuẩn Lập Trình Senior Java Nâng Cao

Tài liệu này tổng hợp các mẫu thiết kế và kỹ thuật lập trình nâng cao bắt buộc áp dụng trong **SSO Platform**. Mục tiêu là loại bỏ code mùi, thể hiện tư duy Senior/Staff Engineer khi phỏng vấn.

---

## 1. Strategy Pattern — Xử Lý Đa Phương Thức (OAuth2 Providers, Payment Methods)

### Vấn đề:
Khi hệ thống hỗ trợ nhiều OAuth2 provider (Google, GitHub, SSO nội bộ) hoặc nhiều phương thức thanh toán, viết `if-else` dài vi phạm **Open-Closed Principle**.

### Giải pháp:

```java
/**
 * Interface chiến lược xử lý OAuth2 token cho từng provider.
 *
 * Tại sao dùng Strategy Pattern?
 * - Khi thêm provider mới (Facebook, Apple), chỉ cần thêm class mới, không sửa code cũ
 * - Open-Closed Principle: mở rộng bằng thêm mới, không sửa đổi
 * - Spring DI tự inject tất cả implementations → Map lookup O(1)
 *
 * @since Sprint 02
 */
public interface OAuth2UserInfoExtractor {
    /** @return tên provider, ví dụ: "google", "github", "sso" */
    String getProviderName();
    UserInfo extractUserInfo(OAuth2User oAuth2User);
}

@Component
public class GoogleUserInfoExtractor implements OAuth2UserInfoExtractor {
    @Override
    public String getProviderName() { return "google"; }

    @Override
    public UserInfo extractUserInfo(OAuth2User oAuth2User) {
        return new UserInfo(
            (String) oAuth2User.getAttributes().get("sub"),
            (String) oAuth2User.getAttributes().get("email"),
            (String) oAuth2User.getAttributes().get("name")
        );
    }
}

@Service
@RequiredArgsConstructor
public class OAuth2UserInfoService {

    private final List<OAuth2UserInfoExtractor> extractors;
    private Map<String, OAuth2UserInfoExtractor> extractorMap;

    /**
     * Khởi tạo Map từ List<Extractor> khi bean được tạo.
     * Tại sao @PostConstruct?
     * - Đảm bảo DI đã hoàn tất trước khi xây dựng Map
     * - Lookup O(1) thay vì stream().filter() O(n) trên mỗi request
     */
    @PostConstruct
    void init() {
        extractorMap = extractors.stream()
            .collect(Collectors.toMap(
                OAuth2UserInfoExtractor::getProviderName,
                e -> e
            ));
    }

    public UserInfo extract(String provider, OAuth2User oAuth2User) {
        OAuth2UserInfoExtractor extractor = extractorMap.get(provider.toLowerCase());
        if (extractor == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                "OAuth2 provider không được hỗ trợ: " + provider);
        }
        return extractor.extractUserInfo(oAuth2User);
    }
}
```

---

## 2. Custom Validation Annotations (JSR-380)

### Vấn đề:
Validation phức tạp như kiểm tra enum hợp lệ, password strength không thể dùng annotation mặc định.

```java
/**
 * Annotation tự chế kiểm tra giá trị thuộc tập hợp Enum hợp lệ.
 *
 * Ví dụ sử dụng:
 *   @ValidEnum(enumClass = Role.class)
 *   String role;
 *
 * Tại sao không dùng @Pattern với regex?
 * - Enum thay đổi sẽ không cần sửa regex → ít lỗi hơn
 * - Thông báo lỗi rõ ràng hơn
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValidator.class)
@Documented
public @interface ValidEnum {
    Class<? extends Enum<?>> enumClass();
    String message() default "Giá trị không nằm trong danh sách cho phép";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {
    private Set<String> acceptedValues;

    @Override
    public void initialize(ValidEnum annotation) {
        acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // Dùng @NotNull riêng nếu muốn bắt buộc
        return acceptedValues.contains(value.toUpperCase());
    }
}

// Áp dụng trong DTO
public record AssignRoleRequest(
    @NotNull UUID userId,

    @NotBlank
    @ValidEnum(enumClass = Role.class, message = "Role phải là: ADMIN, MANAGER, STAFF, AUDITOR, USER, SUPPORT")
    String role
) {}
```

---

## 3. AOP — Audit Log Tự Động (@Auditable)

```java
/**
 * Annotation đánh dấu method cần ghi audit log tự động.
 * Dùng với AuditLogAspect để tách biệt cross-cutting concern.
 *
 * Tại sao AOP thay vì gọi auditService.log() thủ công?
 * - Developer không thể quên gọi log
 * - Business logic sạch, không lẫn với infrastructure concerns
 * - Dễ thay đổi audit strategy mà không sửa business code
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    /** Tên hành động, ví dụ: "USER_ROLE_CHANGED" */
    String action();
    /** Tên resource, ví dụ: "User", "Order" */
    String resource() default "";
}

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest httpRequest;

    /**
     * Intercept các method có @Auditable, ghi log cả thành công lẫn thất bại.
     *
     * Tại sao dùng @Around thay vì @AfterReturning?
     * - @Around cho phép bắt exception và ghi log ngay cả khi method fail
     * - Có thể đo thời gian thực thi
     */
    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        String actorId = extractActorIdFromRequest();
        long startTime = System.currentTimeMillis();
        boolean success = true;

        try {
            Object result = pjp.proceed();
            return result;
        } catch (Throwable ex) {
            success = false;
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            saveAuditLog(auditable, actorId, success, duration);
        }
    }

    private String extractActorIdFromRequest() {
        // Đọc từ X-User-Id header (đã được Gateway inject sau JWT validation)
        String userId = httpRequest.getHeader("X-User-Id");
        return userId != null ? userId : "anonymous";
    }

    private void saveAuditLog(Auditable auditable, String actorId,
                               boolean success, long durationMs) {
        try {
            AuditLog log = AuditLog.builder()
                .actorId(actorId)
                .action(auditable.action())
                .resource(auditable.resource())
                .success(success)
                .durationMs(durationMs)
                .ipAddress(httpRequest.getRemoteAddr())
                .createdAt(Instant.now())
                .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Không để audit log failure ảnh hưởng business logic
            log.error("Không thể ghi audit log cho action: {}", auditable.action(), e);
        }
    }
}

// Cách sử dụng trong Service
@Service
public class UserService {

    @Auditable(action = "USER_ROLE_CHANGED", resource = "User")
    @Transactional
    public void assignRole(UUID userId, String role) {
        // Business logic không cần biết về audit log
    }
}
```

---

## 4. AOP — Idempotency Check (@Idempotent)

```java
/**
 * Annotation đánh dấu endpoint cần kiểm tra Idempotency-Key.
 * Ngăn chặn double-submit khi user click nhiều lần.
 *
 * Tại sao Redis thay vì DB?
 * - Atomic SETNX operation → không race condition
 * - TTL tự động dọn sạch → không cần cleanup job
 * - Tốc độ O(1) → không làm chậm request
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String keyPrefix() default "idempotency:";
    long ttlSeconds() default 300; // 5 phút
}

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private final StringRedisTemplate redis;
    private final HttpServletRequest request;

    @Around("@annotation(idempotent)")
    public Object enforceIdempotency(ProceedingJoinPoint pjp, Idempotent idempotent)
            throws Throwable {
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                "Thiếu header 'Idempotency-Key' bắt buộc");
        }

        String redisKey = idempotent.keyPrefix() + key;

        // SETNX atomic — chỉ set nếu key chưa tồn tại
        Boolean isNew = redis.opsForValue().setIfAbsent(
            redisKey, "PROCESSING",
            Duration.ofSeconds(idempotent.ttlSeconds())
        );

        if (Boolean.FALSE.equals(isNew)) {
            log.warn("Phát hiện request trùng lặp, Idempotency-Key: {}", key);
            throw new BusinessException(ErrorCode.CONFLICT,
                "Request trùng lặp. Giao dịch đang được xử lý hoặc đã hoàn tất.");
        }

        try {
            Object result = pjp.proceed();
            redis.opsForValue().set(redisKey, "DONE",
                Duration.ofSeconds(idempotent.ttlSeconds()));
            return result;
        } catch (Throwable ex) {
            // Nếu lỗi → xóa key để client có thể retry
            redis.delete(redisKey);
            throw ex;
        }
    }
}
```

---

## 5. MDC Context Propagation (Distributed Tracing)

```java
/**
 * Cấu hình Async Executor bảo toàn MDC context qua Virtual Thread boundary.
 *
 * Vấn đề: MDC dùng ThreadLocal → khi spawn Virtual Thread mới, traceId bị mất.
 * Giải pháp: Copy MDC map sang thread mới trước khi chạy.
 *
 * Tại sao Virtual Threads (Java 21)?
 * - Xử lý hàng nghìn concurrent requests với chi phí bộ nhớ thấp hơn Platform Threads
 * - Không block thread khi chờ I/O (DB query, HTTP call)
 * - Spring Boot 3.2+ hỗ trợ Virtual Threads natively
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "tracingTaskExecutor")
    public Executor tracingTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("vt-async-");
        executor.setVirtualThreads(true); // Java 21 Virtual Threads

        // Wrap để copy MDC sang mỗi Virtual Thread mới
        return command -> {
            Map<String, String> mdcContext = MDC.getCopyOfContextMap();
            executor.execute(() -> {
                try {
                    if (mdcContext != null) {
                        MDC.setContextMap(mdcContext);
                    }
                    command.run();
                } finally {
                    MDC.clear(); // Quan trọng: giải phóng bộ nhớ ThreadLocal
                }
            });
        };
    }
}
```

---

## 6. Specification Pattern — Dynamic Query Builder

```java
/**
 * Specification Pattern cho phép xây dựng query động không cần viết JPQL thủ công.
 *
 * Tại sao không viết thẳng JPQL/SQL?
 * - Filter có thể null → phải kiểm tra từng param trong JPQL rất rườm rà
 * - Specification có thể combine (and, or) → flexible
 * - Testable: unit test từng Specification riêng lẻ
 *
 * @since Sprint 13 - Product Search
 */
public class ProductSpecification {

    /**
     * Lọc sản phẩm theo tên (LIKE, case-insensitive).
     * Trả về null nếu keyword rỗng → JPA Repository sẽ bỏ qua condition này.
     */
    public static Specification<Product> hasNameLike(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            return cb.like(
                cb.lower(root.get("name")),
                "%" + keyword.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isBlank()) return null;
            return cb.equal(root.get("category"), category);
        };
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Product> hasPriceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("price"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("price"), min);
            return cb.between(root.get("price"), min, max);
        };
    }
}

// Repository
public interface ProductRepository extends JpaRepository<Product, Long>,
    JpaSpecificationExecutor<Product> {}

// Service
@Service
public class ProductService {

    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public Page<ProductResponse> searchProducts(ProductSearchRequest req, Pageable pageable) {
        Specification<Product> spec = Specification
            .where(ProductSpecification.isActive())
            .and(ProductSpecification.hasNameLike(req.keyword()))
            .and(ProductSpecification.hasCategory(req.category()))
            .and(ProductSpecification.hasPriceBetween(req.minPrice(), req.maxPrice()));

        return productRepository.findAll(spec, pageable)
            .map(productMapper::toResponse);
    }
}
```

---

## 7. Builder Pattern Cho Complex Objects

```java
/**
 * Builder Pattern cho Email notification payload.
 * Dùng Lombok @Builder để tránh viết Builder thủ công.
 *
 * Tại sao Builder thay vì constructor nhiều tham số?
 * - Constructor 8 tham số → dễ nhầm thứ tự, khó đọc
 * - Builder → explicit, self-documenting, optional fields rõ ràng
 */
@Builder
@Getter
public class EmailNotification {
    private final String to;
    private final String subject;
    private final String templateId;           // Tên template email
    private final Map<String, String> variables; // Biến truyền vào template

    @Builder.Default
    private final boolean html = true;

    @Builder.Default
    private final int priority = 5;            // 1 (cao) → 10 (thấp)
}

// Cách dùng — rõ ràng, dễ đọc
EmailNotification email = EmailNotification.builder()
    .to(user.getEmail())
    .subject("Xác nhận đơn hàng #" + order.getOrderCode())
    .templateId("order-confirmation")
    .variables(Map.of(
        "userName", user.getDisplayName(),
        "orderCode", order.getOrderCode(),
        "total", formatCurrency(order.getTotalAmount())
    ))
    .priority(3)
    .build();
```

---

## 8. Transactional Outbox Pattern

```java
/**
 * Transactional Outbox Pattern đảm bảo At-Least-Once delivery.
 *
 * Vấn đề cần giải quyết:
 * 1. DB commit thành công NHƯNG Kafka publish fail → event bị mất
 * 2. Kafka publish thành công NHƯNG DB rollback → ghost event
 *
 * Giải pháp:
 * - Ghi event vào bảng outbox_events CÙNG @Transactional với business entity
 * - Scheduled job đọc outbox → publish Kafka → mark SENT
 * - Atomic: hoặc cả 2 thành công, hoặc cả 2 rollback
 *
 * @since Sprint 16
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxRepo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    /**
     * Chạy mỗi 5 giây để publish pending outbox events lên Kafka.
     *
     * Tại sao @Scheduled thay vì CDC (Debezium)?
     * - Đơn giản hơn, không cần Debezium infrastructure
     * - Phù hợp cho dự án học tập
     * - CDC sẽ dùng trong Sprint cao hơn
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepo.findByStatusOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING, Pageable.ofSize(50)
        );

        for (OutboxEvent event : pending) {
            try {
                kafka.send(event.getEventType().toLowerCase(), event.getPayload())
                     .get(5, TimeUnit.SECONDS); // Sync send với timeout

                event.markAsSent();
                outboxRepo.save(event);

            } catch (Exception ex) {
                log.error("Không thể publish outbox event ID: {}, type: {}",
                    event.getId(), event.getEventType(), ex);

                event.incrementRetryCount();
                if (event.getRetryCount() >= 3) {
                    event.markAsFailed();
                    log.error("Outbox event FAILED sau 3 lần retry: {}", event.getId());
                }
                outboxRepo.save(event);
            }
        }
    }
}
```
