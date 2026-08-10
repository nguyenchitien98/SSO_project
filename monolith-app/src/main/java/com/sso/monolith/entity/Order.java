package com.sso.monolith.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * Thực thể Đại diện cho Đơn hàng (Order) trong cơ sở dữ liệu.
 *
 * <p>Mỗi đơn hàng thuộc sở hữu của một {@link UserProfile} (liên kết với UUID SSO).
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserProfile user;

  @Column(name = "order_code", unique = true, nullable = false, length = 50)
  private String orderCode;

  @Column(nullable = false, length = 50)
  private String status;

  @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "shipping_address", columnDefinition = "TEXT")
  private String shippingAddress;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @Column(name = "idempotency_key", unique = true)
  private String idempotencyKey;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Builder.Default
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }

  /** Thêm chi tiết dòng sản phẩm vào đơn hàng để thiết lập quan hệ hai chiều đồng bộ. */
  public void addItem(OrderItem item) {
    items.add(item);
    item.setOrder(this);
  }
}
