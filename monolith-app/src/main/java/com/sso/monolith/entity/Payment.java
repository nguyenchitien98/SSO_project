package com.sso.monolith.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

/**
 * Thực thể Đại diện cho thông tin Thanh toán (Payment) của đơn hàng.
 *
 * <p>Ánh xạ với bảng `payments` trong CSDL.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 50)
  private String method;

  @Column(nullable = false, length = 50)
  private String status;

  @Column(name = "transaction_ref", length = 255)
  private String transactionRef;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
  }
}
