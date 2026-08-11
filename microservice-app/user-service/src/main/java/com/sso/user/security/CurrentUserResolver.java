package com.sso.user.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Component hỗ trợ trích xuất thông tin người dùng hiện tại từ trusted HTTP Headers.
 *
 * <p>Headers này do API Gateway đảm nhận việc xác thực JWT, bóc tách và inject tin cậy trước khi
 * chuyển tiếp nội bộ.
 *
 * @author SSO Platform Team
 * @since Sprint 12
 */
@Component
public class CurrentUserResolver {

  /**
   * Giải mã headers và đóng gói thành đối tượng CurrentUser.
   *
   * @param request HTTP Servlet Request
   * @return CurrentUser instance hoặc null nếu request chưa được xác thực
   */
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
