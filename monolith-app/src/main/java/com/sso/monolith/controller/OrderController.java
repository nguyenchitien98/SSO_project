package com.sso.monolith.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.monolith.dto.request.CreateOrderRequest;
import com.sso.monolith.dto.response.OrderResponse;
import com.sso.monolith.service.OrderService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cung cấp các API quản lý Đơn hàng (Order) trên Monolith.
 *
 * <p>Tại sao không có @PreAuthorize ở đây? - Để delegating toàn bộ kiểm tra bảo mật chéo và quyền
 * sở hữu (ownership) xuống Service Layer.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

  private final OrderService orderService;

  /** API đặt hàng mới. */
  @PostMapping
  public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateOrderRequest request) {

    UUID userId = UUID.fromString(jwt.getSubject());
    log.info("API POST /api/orders - Yêu cầu đặt hàng của user UUID: {}", userId);

    OrderResponse response = orderService.createOrder(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Đặt hàng thành công", response));
  }

  /** API chi tiết đơn hàng. */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
    log.info("API GET /api/orders/{} - Lấy chi tiết đơn hàng", id);
    OrderResponse response = orderService.getOrderById(id);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * API lấy danh sách đơn hàng.
   *
   * <p>Phân quyền: - ADMIN được xem tất cả đơn hàng hệ thống. - USER thường chỉ được xem các đơn
   * hàng do chính mình đặt.
   */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {

    UUID userId = UUID.fromString(jwt.getSubject());
    boolean isAdmin =
        jwt.getClaimAsStringList("roles") != null
            && jwt.getClaimAsStringList("roles").stream().anyMatch("ADMIN"::equalsIgnoreCase);

    log.info("API GET /api/orders - Yêu cầu lấy danh sách đơn hàng chéo. IsAdmin: {}", isAdmin);

    Page<OrderResponse> page =
        orderService
            .getMyOrders(userId, isAdmin, pageable)
            .map(
                order ->
                    OrderResponse.builder()
                        .id(order.getId())
                        .userId(order.getUser().getId())
                        .orderCode(order.getOrderCode())
                        .status(order.getStatus())
                        .totalAmount(order.getTotalAmount())
                        .shippingAddress(order.getShippingAddress())
                        .notes(order.getNotes())
                        .createdAt(order.getCreatedAt())
                        .updatedAt(order.getUpdatedAt())
                        .build());

    return ResponseEntity.ok(ApiResponse.success(page));
  }

  /** API hủy đơn hàng. */
  @PostMapping("/{id}/cancel")
  public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long id) {
    log.warn("API POST /api/orders/{}/cancel - Yêu cầu hủy đơn hàng", id);
    OrderResponse response = orderService.cancelOrder(id);
    return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", response));
  }
}
