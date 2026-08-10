package com.sso.monolith.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

/**
 * DTO yêu cầu xử lý thanh toán đơn hàng.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessPaymentRequest {

  @NotNull(message = "ID đơn hàng không được để trống")
  private Long orderId;

  @NotNull(message = "Số tiền thanh toán không được để trống")
  @DecimalMin(value = "0.01", message = "Số tiền thanh toán tối thiểu là 0.01")
  private BigDecimal amount;

  @NotBlank(message = "Phương thức thanh toán không được để trống")
  private String method;
}
