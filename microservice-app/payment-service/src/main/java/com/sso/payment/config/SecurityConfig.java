package com.sso.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình an ninh Spring Security cho Payment Service đóng vai trò là OAuth2 Resource Server.
 *
 * @author SSO Platform Team
 * @since Sprint 15
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  /**
   * Cấu hình filter chain phân quyền các endpoints.
   *
   * @param http HttpSecurity
   * @return SecurityFilterChain instance
   * @throws Exception lỗi cấu hình
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // Endpoints hoàn tiền yêu cầu role ADMIN hoặc MANAGER của user
            .requestMatchers("/api/payments/*/refund").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
            // Endpoint tạo thanh toán yêu cầu scope payment:write (dành cho client credentials của order-service)
            .requestMatchers("/api/payments").hasAuthority("SCOPE_payment:write")
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
    return http.build();
  }
}
