package com.sso.order.security;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

/**
 * Dịch vụ kiểm tra và phân quyền người dùng.
 *
 * @author SSO Platform Team
 * @since Sprint 12
 */
@Service
public class AuthorizationService {

  public void requireRole(CurrentUser user, String role) {
    if (user == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Yêu cầu đăng nhập trước khi thao tác");
    }
    if (user.roles() == null || !user.roles().contains(role)) {
      throw new BusinessException(
          ErrorCode.FORBIDDEN, "Không có quyền thực hiện hành động này (Thiếu vai trò: " + role + ")");
    }
  }

  public void requirePermission(CurrentUser user, String permission) {
    if (user == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Yêu cầu đăng nhập trước khi thao tác");
    }
    if (user.permissions() == null || !user.permissions().contains(permission)) {
      throw new BusinessException(
          ErrorCode.FORBIDDEN,
          "Không có quyền thực hiện hành động này (Thiếu quyền: " + permission + ")");
    }
  }

  public void requireOwnerOrAdmin(CurrentUser user, String resourceUserId) {
    if (user == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Yêu cầu đăng nhập trước khi thao tác");
    }
    if (user.id() == null || (!user.id().equals(resourceUserId) && !user.roles().contains("ADMIN"))) {
      throw new BusinessException(
          ErrorCode.FORBIDDEN, "Không có quyền thao tác trên tài nguyên của người dùng khác");
    }
  }
}
