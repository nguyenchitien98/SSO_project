package com.sso.order.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.order.dto.CreateOrderRequest;
import com.sso.order.entity.Order;
import com.sso.order.security.AuthorizationService;
import com.sso.order.security.CurrentUser;
import com.sso.order.security.CurrentUserResolver;
import com.sso.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller tiếp nhận các yêu cầu nghiệp vụ liên quan tới Đơn hàng (Order Controller).
 *
 * @author SSO Platform Team
 * @since Sprint 14
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

  private final OrderService orderService;
  private final CurrentUserResolver userResolver;
  private final AuthorizationService authService;

  /**
   * Tạo đơn hàng mới (Yêu cầu quyền ORDER_CREATE và Idempotency-Key header).
   *
   * @param request HTTP request chứa headers
   * @param orderReq Thông tin đơn hàng
   * @return ResponseEntity chứa đơn hàng đã đặt thành công
   */
  @PostMapping
  public ResponseEntity<ApiResponse<?>> createOrder(
      HttpServletRequest request, @RequestBody CreateOrderRequest orderReq) {
    CurrentUser currentUser = userResolver.resolve(request);
    String idempotencyKey = request.getHeader("Idempotency-Key");
    
    log.info("API POST /api/orders - User: {}, Idempotency-Key: {}", 
        currentUser != null ? currentUser.email() : "GUEST", idempotencyKey);

    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }

    authService.requirePermission(currentUser, "ORDER_CREATE");

    Order order = orderService.createOrder(orderReq, idempotencyKey, UUID.fromString(currentUser.id()));
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đặt hàng thành công", order));
  }

  @GetMapping("/reports")
  public ResponseEntity<ApiResponse<?>> getReports(HttpServletRequest request) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API GET /api/orders/reports - User: {}", currentUser != null ? currentUser.email() : "GUEST");

    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }

    authService.requireRole(currentUser, "ADMIN");

    java.util.Map<String, Object> reportData = orderService.getReportData();
    return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo doanh thu thành công", reportData));
  }

  /**
   * Lấy danh sách đơn hàng phân trang (Người dùng xem đơn của họ, ADMIN/MANAGER/SUPPORT xem tất cả).
   *
   * @param request HTTP request
   * @param page Số trang
   * @param size Kích thước trang
   * @return ResponseEntity chứa trang đơn hàng
   */
  @GetMapping
  public ResponseEntity<ApiResponse<?>> getOrders(
      HttpServletRequest request,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API GET /api/orders - User: {}", currentUser != null ? currentUser.email() : "GUEST");

    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }

    Page<Order> orders;
    boolean hasElevatedPrivileges = currentUser.roles().contains("ADMIN")
        || currentUser.roles().contains("MANAGER")
        || currentUser.roles().contains("SUPPORT");

    if (hasElevatedPrivileges) {
      orders = orderService.getAllOrders(PageRequest.of(page, size));
    } else {
      orders = orderService.getOrdersForUser(UUID.fromString(currentUser.id()), PageRequest.of(page, size));
    }

    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", orders));
  }

  /**
   * Lấy chi tiết đơn hàng (Chỉ cho phép Owner của đơn hoặc ADMIN/SUPPORT).
   *
   * @param request HTTP request
   * @param id ID đơn hàng
   * @return ResponseEntity chứa thông tin đơn hàng
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<?>> getOrderById(
      HttpServletRequest request, @PathVariable Long id) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API GET /api/orders/{} - User: {}", id, currentUser != null ? currentUser.email() : "GUEST");

    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }

    Order order = orderService.getOrderById(id);
    
    // Kiểm tra quyền sở hữu hoặc vai trò quản trị/hỗ trợ
    boolean isElevated = currentUser.roles().contains("ADMIN") || currentUser.roles().contains("SUPPORT");
    if (!isElevated) {
      authService.requireOwnerOrAdmin(currentUser, order.getUserId().toString());
    }

    return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn hàng thành công", order));
  }

  /**
   * Hủy đơn hàng (Chỉ cho phép Owner của đơn hoặc ADMIN, đơn phải ở trạng thái PENDING).
   *
   * @param request HTTP request
   * @param id ID đơn hàng cần hủy
   * @return ResponseEntity chứa thông tin đơn hàng đã hủy
   */
  @PostMapping("/{id}/cancel")
  public ResponseEntity<ApiResponse<?>> cancelOrder(
      HttpServletRequest request, @PathVariable Long id) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API POST /api/orders/{}/cancel - User: {}", id, currentUser != null ? currentUser.email() : "GUEST");

    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }

    Order order = orderService.getOrderById(id);

    // Kiểm tra quyền sở hữu hoặc vai trò ADMIN
    authService.requireOwnerOrAdmin(currentUser, order.getUserId().toString());

    Order cancelledOrder = orderService.cancelOrder(id);
    return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", cancelledOrder));
  }
}
