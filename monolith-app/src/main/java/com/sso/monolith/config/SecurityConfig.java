package com.sso.monolith.config;

import com.sso.monolith.security.SsoJwtGrantedAuthoritiesConverter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cấu hình bảo mật Spring Security cho ứng dụng Monolith App.
 *
 * <p>Tại sao cần cấu hình này? - Bật `@EnableMethodSecurity` để hỗ trợ kiểm soát phân quyền chéo
 * bằng các anotation `@PreAuthorize` tại tầng Service/Controller. - Cấu hình CORS để cho phép
 * Client Web (Next.js chạy trên port 3000) gọi REST APIs chéo. - Tích hợp xác thực Access Token JWT
 * và áp dụng custom {@link SsoJwtGrantedAuthoritiesConverter} để ánh xạ quyền hạn chi tiết.
 *
 * @author SSO Platform Team
 * @since Sprint 06
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  /** Định nghĩa Security Filter Chain bảo vệ các REST APIs của Monolith. */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable()) // Tắt CSRF để thuận tiện cho việc gọi REST APIs
        .cors(Customizer.withDefaults()) // Áp dụng cấu hình CORS
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/actuator/**", "/error")
                    .permitAll() // Công khai metrics & trang báo lỗi
                    .anyRequest()
                    .authenticated() // Tất cả các request khác yêu cầu JWT hợp lệ
            )
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

    return http.build();
  }

  /**
   * Đăng ký converter cấu hình cho Resource Server, sử dụng cơ chế map quyền hạn từ Access Token.
   */
  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new SsoJwtGrantedAuthoritiesConverter());
    return converter;
  }

  /** Cấu hình chia sẻ tài nguyên chéo nguồn (CORS) cho Frontend Dev Server. */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:3000")); // Cho phép ReactJS / NextJS
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "x-requested-with"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
