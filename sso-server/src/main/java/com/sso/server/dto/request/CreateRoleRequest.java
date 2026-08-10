package com.sso.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO yêu cầu tạo vai trò (Role) mới.
 *
 * <p>Chứa thông tin cơ bản về tên vai trò và mô tả chi tiết.
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoleRequest {

  @NotBlank(message = "Tên vai trò không được để trống")
  @Size(min = 2, max = 50, message = "Tên vai trò phải từ 2 đến 50 ký tự")
  private String name;

  @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
  private String description;
}
