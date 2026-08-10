package com.sso.monolith.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.*;

/**
 * DTO phản hồi thông tin chi tiết của đơn hàng.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
  private Long id;
  private UUID userId;
  private String orderCode;
  private String status;
  private BigDecimal totalAmount;
  private String shippingAddress;
  private String notes;
  private Instant createdAt;
  private Instant updatedAt;
  private List<OrderItemResponse> items;

  /** DTO chi tiết từng dòng sản phẩm trong đơn hàng. */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
  }
}
