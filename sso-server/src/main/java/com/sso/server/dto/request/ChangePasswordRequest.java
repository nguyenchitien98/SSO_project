package com.sso.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO yêu cầu thay đổi mật khẩu tài khoản.
 *
 * @author SSO Platform Team
 * @since Sprint 04
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

  @NotBlank(message = "Mật khẩu cũ không được để trống")
  private String oldPassword;

  @NotBlank(message = "Mật khẩu mới không được để trống")
  @Size(min = 6, max = 100, message = "Mật khẩu mới phải từ 6 đến 100 ký tự")
  private String newPassword;
}
