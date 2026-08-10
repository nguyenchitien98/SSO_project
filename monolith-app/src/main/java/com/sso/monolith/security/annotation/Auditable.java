package com.sso.monolith.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation đánh dấu các phương thức nghiệp vụ nhạy cảm cần được tự động ghi nhận lịch sử (Audit
 * Logging).
 *
 * <p>Được kết hợp với lập trình hướng khía cạnh (AOP) qua `AuditLogAspect` để tự động chụp thông
 * tin actor, IP, hành động và thực thể liên quan chéo hệ thống.
 *
 * @author SSO Platform Team
 * @since Sprint 09
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {

  /** Tên hành động thực hiện (Ví dụ: `PRODUCT_CREATE`, `ORDER_CANCEL`). */
  String action();

  /** Tên thực thể / tài nguyên tác động (Ví dụ: `Product`, `Order`). */
  String resource();
}
