package com.sso.server.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO yêu cầu cập nhật thông tin người dùng.
 *
 * <p>Cho phép Admin cập nhật các thông tin cơ bản của người dùng như email, họ tên, và trạng thái
 * kích hoạt tài khoản.
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

  @NotBlank(message = "Email không được để trống")
  @Email(message = "Email không đúng định dạng")
  @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
  private String email;

  @Size(max = 100, message = "Tên không được vượt quá 100 ký tự")
  private String firstName;

  @Size(max = 100, message = "Họ không được vượt quá 100 ký tự")
  private String lastName;

  private boolean enabled;
}
