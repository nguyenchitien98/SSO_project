package com.sso.server.dto.response;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.*;

/**
 * DTO phản hồi thông tin chi tiết người dùng.
 *
 * <p>Trả về thông tin hồ sơ tài khoản cùng danh sách các vai trò đã được gán.
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
  private UUID id;
  private String username;
  private String email;
  private String firstName;
  private String lastName;
  private boolean enabled;
  private boolean locked;
  private String lockedReason;
  private Instant lastLoginAt;
  private Set<String> roles;
  private Instant createdAt;
  private Instant updatedAt;
}
