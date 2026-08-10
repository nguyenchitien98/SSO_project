package com.sso.monolith.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

/**
 * DTO yêu cầu cập nhật thông tin sản phẩm.
 *
 * @author SSO Platform Team
 * @since Sprint 07
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductRequest {

  @NotBlank(message = "Tên sản phẩm không được để trống")
  private String name;

  private String description;

  @NotNull(message = "Giá sản phẩm không được để trống")
  @DecimalMin(value = "0.0", inclusive = true, message = "Giá sản phẩm phải lớn hơn hoặc bằng 0")
  private BigDecimal price;

  @NotNull(message = "Số lượng tồn kho không được để trống")
  @Min(value = 0, message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0")
  private Integer stock;

  private String category;

  private String imageUrl;

  @NotNull(message = "Trạng thái hoạt động không được để trống")
  private Boolean active;
}
