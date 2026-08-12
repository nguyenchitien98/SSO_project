package com.sso.order.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

/**
 * Feign Request Interceptor tự động thêm Bearer Token cho các cuộc gọi Service-to-Service.
 *
 * <p>Tại sao sử dụng interceptor này?
 * - Tự động hóa việc gắn Authorization header khi gọi sang payment-service.
 * - Giải phóng logic nghiệp vụ khỏi việc tự quản lý và làm mới OAuth2 access tokens.
 *
 * @author SSO Platform Team
 * @since Sprint 15
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2FeignRequestInterceptor implements RequestInterceptor {

  private final OAuth2AuthorizedClientManager authorizedClientManager;

  @Override
  public void apply(RequestTemplate template) {
    // Chỉ chèn Access Token khi gọi sang payment-service
    if ("payment-service".equals(template.feignTarget().name())) {
      log.info("[S2S Auth] Thực hiện lấy/làm mới OAuth2 Client Credentials token cho order-service...");
      
      OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
          .withClientRegistrationId("payment-service-client")
          .principal("order-service") // Tên định danh đại diện cho service
          .build();

      OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
      
      if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
        String token = authorizedClient.getAccessToken().getTokenValue();
        template.header("Authorization", "Bearer " + token);
        log.info("[S2S Auth] Đã inject thành công Bearer token vào HTTP Headers gọi payment-service.");
      } else {
        log.error("[S2S Auth] Không thể lấy được OAuth2 access token từ SSO Server!");
      }
    }
  }
}
