package com.sso.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Cấu hình Rate Limiting (Giới hạn tần suất yêu cầu) cho API Gateway.
 *
 * <p>Khai báo Bean KeyResolver định nghĩa khóa phân biệt các yêu cầu để áp dụng giới hạn.
 *
 * @author SSO Platform Team
 * @since Sprint 11
 */
@Configuration
public class RateLimiterConfig {

  /**
   * Khai báo KeyResolver sử dụng định danh người dùng X-User-Id (được Gateway inject bảo mật).
   *
   * <p>Nếu yêu cầu chưa xác thực hoặc không có X-User-Id, fallback sang địa chỉ IP của Client.
   *
   * @return KeyResolver instance
   */
  @Bean
  @Primary
  public KeyResolver userKeyResolver() {
    return exchange ->
        Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
            .defaultIfEmpty(
                exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown-ip");
  }
}
