package com.sso.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO chứa thông tin yêu cầu thanh toán đơn hàng (Payment Request DTO).
 *
 * @author SSO Platform Team
 * @since Sprint 15
 */
public record PaymentRequestDto(
    Long orderId,
    UUID userId,
    BigDecimal amount,
    String method
) {}
