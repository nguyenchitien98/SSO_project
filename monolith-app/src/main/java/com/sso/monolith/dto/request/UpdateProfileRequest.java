package com.sso.monolith.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO yêu cầu cập nhật hồ sơ cá nhân người dùng.
 *
 * @author SSO Platform Team
 * @since Sprint 06
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

  @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự")
  private String displayName;

  @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
  private String phone;

  @Size(max = 500, message = "Đường dẫn ảnh đại diện tối đa 500 ký tự")
  private String avatarUrl;

  private String address;

  private String preferences;
}
