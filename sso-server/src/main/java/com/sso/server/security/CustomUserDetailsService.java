package com.sso.server.security;

import com.sso.server.entity.User;
import com.sso.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp dịch vụ tải thông tin xác thực người dùng (UserDetailsService Custom) từ cơ sở dữ liệu.
 *
 * <p>Sử dụng {@link UserRepository} để truy vấn thông tin tài khoản người dùng từ PostgreSQL và
 * đóng gói kết quả vào đối tượng {@link SsoUserDetails} cho Spring Security.
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private final BruteForceProtectionService bruteForceProtectionService;

  /**
   * Tải thông tin chi tiết người dùng dựa trên tên đăng nhập.
   *
   * @param username Tên đăng nhập của người dùng cần xác thực
   * @return Đối tượng UserDetails chứa dữ liệu tài khoản và danh sách quyền hạn
   * @throws UsernameNotFoundException Nếu không tìm thấy username trong DB
   */
  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.info("Xác thực yêu cầu đăng nhập cho người dùng: {}", username);

    // Kiểm tra trạng thái khóa tài khoản (Brute Force Protection)
    if (bruteForceProtectionService.isBlocked(username)) {
      log.warn("Đăng nhập bị từ chối: Tài khoản {} đang bị khóa bảo mật", username);
      throw new org.springframework.security.authentication.LockedException(
          "Tài khoản đã bị tạm khóa (30 phút) hoặc khóa vĩnh viễn do thử sai mật khẩu nhiều lần.");
    }

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> {
                  log.warn("Đăng nhập thất bại: Không tìm thấy username {}", username);
                  return new UsernameNotFoundException(
                      "Không tìm thấy người dùng có tên đăng nhập: " + username);
                });

    return new SsoUserDetails(user);
  }
}
