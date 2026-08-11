package com.sso.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filter loại bỏ toàn bộ Header có tiền tố 'X-User-' từ phía Client gửi lên.
 *
 * <p>Vai trò: Bảo vệ Trust Boundary. Ngăn ngừa việc Attacker cố tình chèn các Header như
 * X-User-Id: 1 (ADMIN) hòng vượt qua cơ chế phân quyền (Privilege Escalation).
 *
 * @author SSO Platform Team
 * @since Sprint 11
 */
@Component
@Slf4j
public class StripClientHeadersFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    log.debug("Global Filter - StripClientHeadersFilter - Khởi chạy kiểm tra Header");

    ServerHttpRequest request = exchange.getRequest();
    ServerHttpRequest.Builder requestBuilder = request.mutate();

    // Loại bỏ tất cả Header có tiền tố 'x-user-' (không phân biệt hoa thường)
    request
        .getHeaders()
        .keySet()
        .forEach(
            headerName -> {
              if (headerName.toLowerCase().startsWith("x-user-")) {
                log.warn(
                    "CẢNH BÁO: Phát hiện Client gửi Header bảo mật nhạy cảm: {} = {}. Tiến hành strip.",
                    headerName,
                    request.getHeaders().getFirst(headerName));
                requestBuilder.headers(httpHeaders -> httpHeaders.remove(headerName));
              }
            });

    return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
  }

  @Override
  public int getOrder() {
    // Chạy ở vị trí cao nhất (Highest Precedence) để làm sạch Request trước mọi xử lý tiếp theo
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
