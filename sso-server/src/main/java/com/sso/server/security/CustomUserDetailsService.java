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
 * <p>Sử dụng {@link UserRepository} để truy vấn thông tin tài khoản người dùng từ PostgreSQL
 * và đóng gói kết quả vào đối tượng {@link SsoUserDetails} cho Spring Security.
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

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

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Đăng nhập thất bại: Không tìm thấy username {}", username);
                    return new UsernameNotFoundException("Không tìm thấy người dùng có tên đăng nhập: " + username);
                });

        return new SsoUserDetails(user);
    }
}
