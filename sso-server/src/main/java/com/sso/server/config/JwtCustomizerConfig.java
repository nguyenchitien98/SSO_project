package com.sso.server.config;

import com.sso.server.entity.User;
import com.sso.server.security.SsoUserDetails;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * Cấu hình bổ sung các claims tùy chỉnh (custom claims) vào cấu trúc Token JWT được mã hóa.
 *
 * <p>Giúp đính kèm thêm các siêu dữ liệu quan trọng như vai trò (`roles`), quyền hạn
 * (`permissions`), email và tên hiển thị đầy đủ của người dùng đăng nhập vào Access Token.
 *
 * <p>Tại sao cần chèn trực tiếp các claims này vào Access Token? - Giúp API Gateway và các Service
 * nội bộ có thể tự động parse và phân quyền ngoại tuyến (offline auth checking) mà không cần phải
 * tạo cuộc gọi mạng ngược lại SSO Server để truy vấn thông tin user. - Tiết kiệm CPU, giảm thiểu độ
 * trễ (latency) của toàn hệ thống phân tán.
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Configuration
@Slf4j
public class JwtCustomizerConfig {

  /**
   * Bean Customizer chịu trách nhiệm chèn dữ liệu tùy chỉnh vào JWT Claims Set.
   *
   * @return OAuth2TokenCustomizer instance
   */
  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
    return context -> {
      if ("access_token".equals(context.getTokenType().getValue())) {
        log.info(
            "Thực hiện tùy biến chèn custom claims vào Access Token cho client {}",
            context.getRegisteredClient().getClientId());

        Object principal = context.getPrincipal().getPrincipal();

        if (principal instanceof SsoUserDetails ssoUserDetails) {
          User user = ssoUserDetails.getUser();

          // 1. Chèn email của người dùng
          context.getClaims().claim("email", user.getEmail());

          // 2. Chèn tên đầy đủ (firstName + lastName)
          String fullName =
              String.format(
                      "%s %s",
                      user.getFirstName() != null ? user.getFirstName() : "",
                      user.getLastName() != null ? user.getLastName() : "")
                  .trim();
          context.getClaims().claim("name", fullName.isEmpty() ? user.getUsername() : fullName);

          // 3. Chèn danh sách vai trò (roles) dưới dạng mảng JSON
          Set<String> roles =
              user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet());
          context.getClaims().claim("roles", roles);

          // 4. Chèn danh sách các quyền cụ thể (permissions)
          Set<String> permissions =
              ssoUserDetails.getAuthorities().stream()
                  .map(GrantedAuthority::getAuthority)
                  .filter(
                      auth ->
                          !auth.startsWith("ROLE_")) // Lọc bỏ role prefix để lấy riêng permissions
                  .collect(Collectors.toSet());
          context.getClaims().claim("permissions", permissions);

          log.debug(
              "Đã inject claims thành công cho user: {}, Roles: {}, Permissions: {}",
              user.getUsername(),
              roles,
              permissions);
        } else {
          // Xử lý luồng Client Credentials (không có human user đăng nhập)
          log.debug("Luồng xác thực Client Credentials - Bỏ qua inject user claims");
          context.getClaims().claim("client_id", context.getRegisteredClient().getClientId());
        }
      }
    };
  }
}
