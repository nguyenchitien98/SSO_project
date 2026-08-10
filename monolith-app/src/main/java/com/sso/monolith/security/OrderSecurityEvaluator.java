package com.sso.monolith.security;

import com.sso.monolith.entity.Order;
import com.sso.monolith.repository.OrderRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Bộ đánh giá bảo mật (Security Evaluator) dùng để kiểm soát Quyền sở hữu tài nguyên (Resource
 * Ownership).
 *
 * <p>Tại sao cần lớp riêng thay vì kiểm tra trực tiếp trong mã nguồn Service? - Cho phép sử dụng
 * trực tiếp cú pháp SpEL trong các anotation `@PreAuthorize` tại tầng Service. Ví dụ:
 * `@PreAuthorize("@orderSecurity.isOwnerOrAdmin(authentication, #orderId)")`. - Phân tách rõ ràng
 * (Separation of Concerns): logic nghiệp vụ bảo mật chéo tách biệt khỏi logic nghiệp vụ của Order.
 * - Dễ dàng mock/đơn giản hóa khi viết unit tests hoặc tích hợp kiểm soát phân quyền ABAC.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Component("orderSecurity")
@RequiredArgsConstructor
@Slf4j
public class OrderSecurityEvaluator {

  private final OrderRepository orderRepository;

  /**
   * Kiểm tra xem người dùng hiện tại có phải là chủ sở hữu đơn hàng hoặc là ADMIN hay không.
   *
   * @param authentication Đối tượng chứa thông tin xác thực hiện tại của Spring Security
   * @param orderId ID đơn hàng cần thao tác chéo
   * @return true nếu hợp lệ, ngược lại false
   */
  public boolean isOwnerOrAdmin(Authentication authentication, Long orderId) {
    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Kiểm tra ownership thất bại: Chưa xác thực");
      return false;
    }

    // 1. Kiểm tra nếu là ADMIN -> Tự động cho phép truy cập tất cả
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .anyMatch(
                grantedAuthority -> "ROLE_ADMIN".equalsIgnoreCase(grantedAuthority.getAuthority()));

    if (isAdmin) {
      log.info("Xác thực thành công: User là ADMIN được quyền truy cập đơn hàng ID: {}", orderId);
      return true;
    }

    // 2. Kiểm tra nếu là người dùng thông thường -> So sánh UUID sở hữu đơn hàng
    Object principal = authentication.getPrincipal();
    if (principal instanceof Jwt jwt) {
      String currentUserId = jwt.getSubject();

      Optional<Order> orderOpt = orderRepository.findById(orderId);
      if (orderOpt.isEmpty()) {
        log.warn("Kiểm tra ownership thất bại: Không tìm thấy đơn hàng với ID: {}", orderId);
        return false;
      }

      Order order = orderOpt.get();
      String ownerId = order.getUser().getId().toString();

      boolean isOwner = currentUserId.equalsIgnoreCase(ownerId);
      if (isOwner) {
        log.info(
            "Xác thực ownership thành công: User {} sở hữu đơn hàng ID: {}",
            currentUserId,
            orderId);
        return true;
      } else {
        log.warn(
            "Xác thực ownership thất bại: User {} cố gắng truy cập đơn hàng ID {} của user {}",
            currentUserId,
            orderId,
            ownerId);
        return false;
      }
    }

    log.warn("Xác thực ownership thất bại: Principal không thuộc kiểu Jwt");
    return false;
  }
}
