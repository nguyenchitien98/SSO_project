package com.sso.monolith.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

/**
 * DTO yêu cầu tạo mới một đơn hàng trong hệ thống Monolith.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

  @NotEmpty(message = "Danh sách sản phẩm trong đơn hàng không được để trống")
  @Valid
  private List<OrderItemRequest> items;

  @NotNull(message = "Địa chỉ giao hàng không được để trống")
  private String shippingAddress;

  private String notes;

  private String idempotencyKey;

  /** DTO đại diện cho một sản phẩm trong giỏ hàng đặt mua. */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class OrderItemRequest {

    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @NotNull(message = "Số lượng sản phẩm không được để trống")
    @Min(value = 1, message = "Số lượng đặt mua tối thiểu là 1")
    private Integer quantity;
  }
}
