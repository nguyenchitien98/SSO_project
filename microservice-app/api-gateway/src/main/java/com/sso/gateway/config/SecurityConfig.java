package com.sso.gateway.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Cấu hình bảo mật phản kháng (Reactive WebFlux Security) cho API Gateway.
 *
 * <p>Quản lý Trust Boundary: chặn toàn bộ request chưa xác thực, cấu hình CORS, giải mã JWT tự động
 * thông qua JWKS endpoint của SSO Server.
 *
 * @author SSO Platform Team
 * @since Sprint 11
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  /**
   * Khai báo chuỗi Filter bảo mật phản kháng SecurityWebFilterChain.
   *
   * @param http ServerHttpSecurity builder
   * @return SecurityWebFilterChain instance
   */
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/actuator/**")
                    .permitAll() // Cho phép truy cập metrics giám sát công khai
                    .anyExchange()
                    .authenticated() // Bắt buộc xác thực với mọi request nghiệp vụ
            )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

    return http.build();
  }

  /**
   * Cấu hình chia sẻ tài nguyên chéo nguồn (CORS) hỗ trợ cổng của Frontend Web.
   *
   * @return CorsConfigurationSource instance
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3001"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "x-requested-with", "idempotency-key"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
