package com.sso.monolith.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

/**
 * DTO phản hồi kết quả thanh toán.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
  private Long id;
  private Long orderId;
  private BigDecimal amount;
  private String method;
  private String status;
  private String transactionRef;
  private Instant createdAt;
}
