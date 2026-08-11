package com.sso.order.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Component hỗ trợ trích xuất thông tin người dùng hiện tại từ trusted HTTP Headers.
 *
 * @author SSO Platform Team
 * @since Sprint 12
 */
@Component
public class CurrentUserResolver {

  public CurrentUser resolve(HttpServletRequest request) {
    String userId = request.getHeader("X-User-Id");
    if (!StringUtils.hasText(userId)) {
      return null;
    }

    String email = request.getHeader("X-User-Email");
    String rolesHeader = request.getHeader("X-User-Roles");
    String permissionsHeader = request.getHeader("X-User-Permissions");

    List<String> roles =
        StringUtils.hasText(rolesHeader)
            ? Arrays.asList(rolesHeader.split(","))
            : Collections.emptyList();

    List<String> permissions =
        StringUtils.hasText(permissionsHeader)
            ? Arrays.asList(permissionsHeader.split(","))
            : Collections.emptyList();

    return new CurrentUser(userId, email, roles, permissions);
  }
}
