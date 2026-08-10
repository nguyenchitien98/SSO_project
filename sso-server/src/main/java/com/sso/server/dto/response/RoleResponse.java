package com.sso.server.dto.response;

import java.util.Set;
import lombok.*;

/**
 * DTO phản hồi thông tin vai trò và danh sách các quyền hạn đi kèm.
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {
  private Long id;
  private String name;
  private String description;
  private Set<String> permissions;
}
