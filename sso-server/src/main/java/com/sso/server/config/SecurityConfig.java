package com.sso.server.config;

import com.sso.server.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình bảo mật Spring Security (User Authentication Security Chain).
 *
 * <p>Định nghĩa chuỗi bộ lọc bảo mật mặc định (Default Security Filter Chain) cho các tương tác
 * người dùng trực tiếp như màn hình Đăng nhập (Form Login), Logout, và xác thực tài khoản dựa trên
 * CSDL PostgreSQL.
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomUserDetailsService userDetailsService;

  /**
   * Cấu hình chuỗi bảo mật mặc định cho các request thông thường (không phải OAuth2 protocol).
   *
   * @param http Đối tượng HttpSecurity để cấu hình các rule bảo mật
   * @return Đối tượng SecurityFilterChain mặc định
   * @throws Exception Lỗi cấu hình bảo mật
   */
  @Bean
  @Order(2)
  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/actuator/**")
                    .permitAll() // Cho phép truy cập metrics giám sát công khai
                    .requestMatchers("/admin/**")
                    .hasAuthority("SCOPE_admin") // Bảo mật API admin
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    Customizer.withDefaults())) // Hỗ trợ xác thực JWT Bearer Token cho Admin APIs
        .formLogin(
            formLogin ->
                formLogin.permitAll() // Sử dụng màn hình login mặc định của Spring Security
            )
        .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());

    return http.build();
  }

  /**
   * Cấu hình Provider kết nối UserDetailsService và PasswordEncoder mã hóa.
   *
   * @return DaoAuthenticationProvider instance
   */
  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  /**
   * Khai báo Bean mã hóa mật khẩu sử dụng thuật toán BCrypt.
   *
   * <p>Mật khẩu của người dùng bắt buộc phải được băm (hashing) trước khi lưu trữ vào database để
   * đảm bảo an toàn, phòng ngừa rò rỉ dữ liệu.
   *
   * @return PasswordEncoder dùng BCrypt
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
