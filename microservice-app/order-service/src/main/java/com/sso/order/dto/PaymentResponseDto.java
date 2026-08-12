package com.sso.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO chứa thông tin phản hồi sau khi thanh toán đơn hàng (Payment Response DTO).
 *
 * @author SSO Platform Team
 * @since Sprint 15
 */
public record PaymentResponseDto(
    UUID id,
    Long orderId,
    BigDecimal amount,
    String method,
    String status,
    Instant createdAt
) {}
