package com.sso.order.dto;

import java.util.List;

/**
 * DTO yêu cầu tạo đơn hàng mới (Create Order Request DTO).
 *
 * @author SSO Platform Team
 * @since Sprint 14
 */
public record CreateOrderRequest(
    String shippingAddress,
    List<OrderItemDto> items
) {
  public record OrderItemDto(
      Long productId,
      Integer quantity
  ) {}
}
