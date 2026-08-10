package com.sso.server.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Custom Handler xử lý sau khi người dùng đăng nhập thành công bằng mật khẩu.
 *
 * <p>Tại sao cần Custom Success Handler? - Để tích hợp luồng xác thực 2 lớp (TOTP 2FA). - Nếu tài
 * khoản của người dùng đã kích hoạt 2FA (totpEnabled == true) ➔ Tạm giữ đối tượng Authentication
 * vào Session, xóa SecurityContext hiện tại (để ngăn bypass xác thực) và chuyển hướng sang màn hình
 * nhập mã OTP `/login/2fa`. - Nếu không kích hoạt 2FA ➔ Cho phép đăng nhập bình thường và chuyển
 * hướng đến trang ban đầu.
 *
 * @author SSO Platform Team
 * @since Sprint 04
 */
@Component
@Slf4j
public class SsoAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

  public static final String TEMP_AUTH_SESSION_KEY = "TEMP_AUTH";

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    Object principal = authentication.getPrincipal();
    if (principal instanceof SsoUserDetails ssoUserDetails) {
      boolean isTotpEnabled = ssoUserDetails.getUser().isTotpEnabled();
      String username = ssoUserDetails.getUsername();

      if (isTotpEnabled) {
        log.info(
            "Người dùng {} đăng nhập mật khẩu đúng, phát hiện bật 2FA. Chuyển hướng sang màn hình xác thực OTP",
            username);

        // 1. Lưu tạm Authentication vào session
        request.getSession().setAttribute(TEMP_AUTH_SESSION_KEY, authentication);

        // 2. Clear SecurityContext để trạng thái là chưa hoàn tất xác thực (stateless step)
        SecurityContextHolder.clearContext();

        // 3. Redirect đến trang xác thực OTP
        getRedirectStrategy().sendRedirect(request, response, "/login/2fa");
        return;
      }
    }

    log.info("Xác thực thành công (không bật 2FA). Định tuyến tiếp tục...");
    super.onAuthenticationSuccess(request, response, authentication);
  }
}
