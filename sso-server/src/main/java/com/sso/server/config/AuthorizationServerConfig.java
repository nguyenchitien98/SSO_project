package com.sso.server.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Cấu hình Spring Authorization Server (OAuth2 & OpenID Connect 1.0 Provider).
 *
 * <p>Định nghĩa chuỗi bộ lọc bảo mật có độ ưu tiên cao nhất (Order(Ordered.HIGHEST_PRECEDENCE))
 * để xử lý tất cả các endpoint giao thức OAuth2 chuẩn hóa bao gồm:
 * <ul>
 *   <li>Authorization Endpoint (`/oauth2/authorize`)</li>
 *   <li>Token Endpoint (`/oauth2/token`)</li>
 *   <li>Token Revocation Endpoint (`/oauth2/revoke`)</li>
 *   <li>JWKS Endpoint (`/oauth2/jwks`)</li>
 *   <li>OIDC Provider Configuration Metadata Endpoint (`/.well-known/openid-configuration`)</li>
 * </ul>
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    /**
     * Cấu hình Security Filter Chain cho các endpoint giao thức Spring Authorization Server.
     *
     * @param http Đối tượng HttpSecurity để cấu hình
     * @return Đối tượng SecurityFilterChain cho Auth Server
     * @throws Exception Lỗi khởi tạo cấu hình
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // Bật hỗ trợ OIDC 1.0 (OpenID Connect)
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        http
                // Khi nhận yêu cầu chưa xác thực từ trình duyệt ở các API bảo mật -> chuyển hướng đến trang login
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                // Hỗ trợ Resource Server để validate ID Token & Access Tokens
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * Khai báo bộ giải mã JWT (JwtDecoder Bean) sử dụng nguồn khóa JWKS của hệ thống.
     *
     * @param jwkSource Nguồn chứa khóa RSA public/private
     * @return Đối tượng JwtDecoder
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * Cấu hình thiết lập mặc định của Authorization Server (đường dẫn các endpoints mặc định).
     *
     * @return Đối tượng AuthorizationServerSettings
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }
}
