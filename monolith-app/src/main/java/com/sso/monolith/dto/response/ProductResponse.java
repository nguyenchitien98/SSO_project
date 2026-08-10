package com.sso.monolith.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * DTO phản hồi thông tin chi tiết sản phẩm.
 *
 * @author SSO Platform Team
 * @since Sprint 07
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
  private Long id;
  private String name;
  private String description;
  private BigDecimal price;
  private Integer stock;
  private String category;
  private String imageUrl;
  private Boolean active;
  private UUID createdBy;
  private Instant createdAt;
  private Instant updatedAt;
}
