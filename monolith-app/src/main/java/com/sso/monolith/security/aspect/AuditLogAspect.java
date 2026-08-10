package com.sso.monolith.security.aspect;

import com.sso.monolith.entity.AuditLog;
import com.sso.monolith.entity.UserProfile;
import com.sso.monolith.repository.AuditLogRepository;
import com.sso.monolith.repository.UserProfileRepository;
import com.sso.monolith.security.annotation.Auditable;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect xử lý tự động ghi nhận lịch sử thao tác (Audit Logging) sử dụng lập trình hướng khía cạnh
 * (AOP).
 *
 * <p>Bắt các phương thức nghiệp vụ nhạy cảm được gắn thẻ {@link Auditable}: - Trích xuất actor
 * (UUID, Email) từ Context bảo mật. - Trích xuất Client IP của yêu cầu HTTP chéo. - Phân tích thực
 * thể ID (Entity ID) dựa trên tham số truyền vào hoặc phản hồi DTO. - Ghi nhận trạng thái THÀNH
 * CÔNG hoặc THẤT BẠI (kèm chi tiết lỗi) vào DB `monolith_db`.
 *
 * @author SSO Platform Team
 * @since Sprint 09
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

  private final AuditLogRepository auditLogRepository;
  private final UserProfileRepository userProfileRepository;

  /** Vòng chặn xử lý quanh các phương thức chứa annotation {@link Auditable}. */
  @Around("@annotation(com.sso.monolith.security.annotation.Auditable)")
  public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {
    Object result;
    String status = "SUCCESS";
    String failureDetails = null;

    // Lấy annotation Auditable động qua signature để tránh lỗi JoinPointMatch parameter binding
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    java.lang.reflect.Method method = methodSignature.getMethod();
    Auditable auditable = method.getAnnotation(Auditable.class);
    if (auditable == null) {
      try {
        java.lang.reflect.Method targetMethod =
            joinPoint
                .getTarget()
                .getClass()
                .getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
        auditable = targetMethod.getAnnotation(Auditable.class);
      } catch (Exception e) {
        // Bỏ qua
      }
    }

    try {
      // 1. Thực thi phương thức nghiệp vụ gốc
      result = joinPoint.proceed();
      return result;
    } catch (Throwable ex) {
      status = "FAILED";
      failureDetails = ex.getMessage();
      throw ex;
    } finally {
      if (auditable != null) {
        try {
          // 2. Thực hiện ghi Audit Log độc lập trong khối finally (không gây ảnh hưởng tới luồng
          // chính)
          saveAuditLog(joinPoint, auditable, status, failureDetails);
        } catch (Exception e) {
          log.error("Lỗi xảy ra trong quá trình ghi Audit Log chéo bằng AOP", e);
        }
      }
    }
  }

  /** Thu thập thông tin và lưu log vào Database. */
  private void saveAuditLog(
      ProceedingJoinPoint joinPoint, Auditable auditable, String status, String failureDetails) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      log.debug("Bỏ qua ghi audit log do request chưa được xác thực (không có principal)");
      return;
    }

    Object principal = auth.getPrincipal();
    if (principal instanceof Jwt jwt) {
      UUID actorId = UUID.fromString(jwt.getSubject());
      String actorEmail = jwt.getClaimAsString("email");

      // Truy vấn hồ sơ người dùng trong Database
      UserProfile actor = userProfileRepository.findById(actorId).orElse(null);
      if (actor == null) {
        log.warn(
            "Không tìm thấy UserProfile trong database cho UUID: {}. Bỏ qua ghi Audit Log",
            actorId);
        return;
      }

      // Trích xuất entity ID
      String entityId = extractEntityId(joinPoint);

      // Thu thập Client IP
      String ipAddress = getClientIp();

      String action = auditable.action();
      String resource = auditable.resource();

      // Đóng gói JSON chi tiết trạng thái mới/cũ hoặc chi tiết lỗi
      String newValues =
          String.format(
              "{\"status\":\"%s\"%s}",
              status,
              failureDetails != null
                  ? String.format(",\"error\":\"%s\"", failureDetails.replace("\"", "\\\""))
                  : "");

      AuditLog auditLog =
          AuditLog.builder()
              .actor(actor)
              .actorEmail(actorEmail != null ? actorEmail : "unknown@sso.com")
              .action(action)
              .entityType(resource)
              .entityId(entityId)
              .newValues(newValues)
              .ipAddress(ipAddress)
              .build();

      auditLogRepository.save(auditLog);
      log.info(
          "AOP Audit Log Saved: Actor={}, Action={}, Resource={}, EntityID={}, Status={}",
          actor.getDisplayName(),
          action,
          resource,
          entityId,
          status);
    }
  }

  /** Phân tích và trích xuất ID thực thể bị tác động từ tham số đầu vào. */
  private String extractEntityId(ProceedingJoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    if (args == null || args.length == 0) {
      return "N/A";
    }

    // Ưu tiên lấy đối số đầu tiên nếu thuộc kiểu Long, Integer hoặc String làm ID thực thể
    Object firstArg = args[0];
    if (firstArg instanceof Long || firstArg instanceof Integer || firstArg instanceof String) {
      return firstArg.toString();
    }

    // Nếu đối số đầu là một UUID
    if (firstArg instanceof UUID) {
      return firstArg.toString();
    }

    // Fallback kiểm tra tên các đối số khác
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String[] parameterNames = signature.getParameterNames();
    for (int i = 0; i < args.length; i++) {
      if (parameterNames != null && i < parameterNames.length) {
        String paramName = parameterNames[i];
        if ("id".equalsIgnoreCase(paramName)
            || "orderId".equalsIgnoreCase(paramName)
            || "productId".equalsIgnoreCase(paramName)) {
          return args[i] != null ? args[i].toString() : "N/A";
        }
      }
    }

    return "N/A";
  }

  /** Lấy IP Address của client gửi request. */
  private String getClientIp() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return "127.0.0.1";
    }
    HttpServletRequest request = attributes.getRequest();
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    return ip;
  }
}
