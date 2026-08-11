package com.sso.gateway.filter;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filter tự động trích xuất Claims từ JWT hợp lệ và inject vào HTTP Headers tin cậy.
 *
 * <p>Headers được inject bao gồm: - `X-User-Id` (UUID) - `X-User-Email` - `X-User-Roles` -
 * `X-User-Permissions`
 *
 * @author SSO Platform Team
 * @since Sprint 11
 */
@Component
@Slf4j
public class InjectTrustedHeadersFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .filter(auth -> auth instanceof JwtAuthenticationToken)
        .cast(JwtAuthenticationToken.class)
        .map(JwtAuthenticationToken::getToken)
        .map(jwt -> injectHeaders(exchange, jwt))
        .defaultIfEmpty(exchange)
        .flatMap(chain::filter);
  }

  private ServerWebExchange injectHeaders(ServerWebExchange exchange, Jwt jwt) {
    String userId = jwt.getSubject();
    String email = jwt.getClaimAsString("email");
    List<String> roles = jwt.getClaimAsStringList("roles");
    List<String> permissions = jwt.getClaimAsStringList("permissions");

    log.debug(
        "Injecting trusted headers - UserId: {}, Email: {}, Roles: {}, Permissions: {}",
        userId,
        email,
        roles,
        permissions);

    return exchange
        .mutate()
        .request(
            builder -> {
              builder.header("X-User-Id", userId);
              builder.header("X-User-Email", email != null ? email : "");
              builder.header("X-User-Roles", roles != null ? String.join(",", roles) : "");
              builder.header(
                  "X-User-Permissions",
                  permissions != null ? String.join(",", permissions) : "");
            })
        .build();
  }

  @Override
  public int getOrder() {
    // Chạy sau khi WebFilter của Spring Security đã hoàn tất việc giải mã và lưu SecurityContext
    return -10;
  }
}
