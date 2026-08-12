package com.sso.order.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object đại diện cho thông tin sản phẩm nhận về từ Product Service.
 *
 * @author SSO Platform Team
 * @since Sprint 14
 */
public record ProductDto(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer stock,
    Boolean active
) {}
