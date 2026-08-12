package com.sso.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thực thể Đơn hàng (Order Entity).
 *
 * @author SSO Platform Team
 * @since Sprint 14
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "order_code", unique = true, nullable = false, length = 50)
  private String orderCode;

  @Column(name = "status", nullable = false, length = 50)
  @Builder.Default
  private String status = "PENDING";

  @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "idempotency_key", unique = true, length = 255)
  private String idempotencyKey;

  @Version
  @Column(name = "version", nullable = false)
  @Builder.Default
  private Integer version = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  @Builder.Default
  private Instant updatedAt = Instant.now();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<OrderItem> items = new ArrayList<>();

  /**
   * Helper method thêm mặt hàng chi tiết vào đơn hàng.
   *
   * @param item Mặt hàng chi tiết cần thêm
   */
  public void addItem(OrderItem item) {
    items.add(item);
    item.setOrder(this);
  }
}
