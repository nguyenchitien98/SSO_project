package com.sso.user.security;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

/**
 * Dịch vụ kiểm tra và phân quyền người dùng (Role & Permission Authorization Service).
 *
 * <p>Cung cấp các helper methods để ném ra BusinessException nếu người dùng không đáp ứng đủ điều
 * kiện phân quyền.
 *
 * @author SSO Platform Team
 * @since Sprint 12
 */
@Service
public class AuthorizationService {

  /**
   * Yêu cầu người dùng hiện tại phải có vai trò (Role) quy định.
   *
   * @param user Đối tượng người dùng hiện tại
   * @param role Tên vai trò cần kiểm tra (ví dụ: ADMIN, MANAGER)
   */
  public void requireRole(CurrentUser user, String role) {
    if (user == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Yêu cầu đăng nhập trước khi thao tác");
    }
    if (user.roles() == null || !user.roles().contains(role)) {
      throw new BusinessException(
          ErrorCode.FORBIDDEN, "Không có quyền thực hiện hành động này (Thiếu vai trò: " + role + ")");
    }
  }

  /**
   * Yêu cầu người dùng hiện tại phải có quyền hạn (Permission) cụ thể.
   *
   * @param user Đối tượng người dùng hiện tại
   * @param permission Quyền hạn cụ thể cần có (ví dụ: PRODUCT_CREATE, ORDER_DELETE)
   */
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
}
