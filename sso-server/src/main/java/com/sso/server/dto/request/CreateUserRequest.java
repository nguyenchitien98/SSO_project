package com.sso.server.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.*;

/**
 * DTO yêu cầu tạo người dùng mới.
 *
 * <p>Chứa thông tin đăng ký cơ bản của người dùng bao gồm username, email, mật khẩu và danh sách
 * các vai trò (roles) được gán.
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

  @NotBlank(message = "Tên đăng nhập không được để trống")
  @Size(min = 4, max = 50, message = "Tên đăng nhập phải từ 4 đến 50 ký tự")
  private String username;

  @NotBlank(message = "Email không được để trống")
  @Email(message = "Email không đúng định dạng")
  @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
  private String email;

  @NotBlank(message = "Mật khẩu không được để trống")
  @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
  private String password;

  @Size(max = 100, message = "Tên không được vượt quá 100 ký tự")
  private String firstName;

  @Size(max = 100, message = "Họ không được vượt quá 100 ký tự")
  private String lastName;

  @NotEmpty(message = "Người dùng phải có ít nhất một vai trò")
  private Set<String> roles;
}
