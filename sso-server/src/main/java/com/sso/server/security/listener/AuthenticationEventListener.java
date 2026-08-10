package com.sso.server.security.listener;

import com.sso.server.security.BruteForceProtectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe sự kiện xác thực (Authentication Events) của Spring Security.
 *
 * <p>Tự động chặn các sự kiện đăng nhập thành công/thất bại để ghi nhận vào {@link
 * BruteForceProtectionService}, giúp chống tấn công Brute-Force.
 *
 * @author SSO Platform Team
 * @since Sprint 04
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationEventListener {

  private final BruteForceProtectionService bruteForceProtectionService;

  /** Xử lý khi đăng nhập thành công. Reset bộ đếm lỗi trên Redis. */
  @EventListener
  public void onSuccess(AuthenticationSuccessEvent event) {
    Object principal = event.getAuthentication().getPrincipal();
    String username;

    if (principal instanceof UserDetails userDetails) {
      username = userDetails.getUsername();
    } else {
      username = principal.toString();
    }

    log.debug("Lắng nghe sự kiện đăng nhập thành công cho user: {}", username);
    bruteForceProtectionService.registerLoginSuccess(username);
  }

  /** Xử lý khi nhập sai mật khẩu (Bad Credentials). Tăng bộ đếm lỗi brute-force. */
  @EventListener
  public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
    String username = event.getAuthentication().getName();
    log.debug("Lắng nghe sự kiện đăng nhập thất bại cho user: {}", username);
    bruteForceProtectionService.registerLoginFailure(username);
  }
}
