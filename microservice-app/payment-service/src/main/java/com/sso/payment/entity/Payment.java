package com.sso.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thực thể Giao dịch thanh toán (Payment Entity).
 *
 * @author SSO Platform Team
 * @since Sprint 15
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(name = "method", nullable = false, length = 50)
  private String method;

  @Column(name = "status", nullable = false, length = 50)
  @Builder.Default
  private String status = "PENDING";

  @Column(name = "transaction_ref", length = 255)
  private String transactionRef;

  @Column(name = "idempotency_key", unique = true, length = 255)
  private String idempotencyKey;

  @Version
  @Column(name = "version", nullable = false)
  @Builder.Default
  private Integer version = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();
}
