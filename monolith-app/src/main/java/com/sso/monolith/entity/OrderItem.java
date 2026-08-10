package com.sso.monolith.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * Thực thể lưu trữ Chi tiết mặt hàng đặt mua trong Đơn hàng.
 *
 * <p>Ánh xạ với bảng `order_items` trong CSDL.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal subtotal;
}
