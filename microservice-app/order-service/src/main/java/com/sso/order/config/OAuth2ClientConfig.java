package com.sso.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/**
 * Cấu hình OAuth2 Client phục vụ luồng Service-to-Service client_credentials.
 *
 * @author SSO Platform Team
 * @since Sprint 15
 */
@Configuration
public class OAuth2ClientConfig {

  /**
   * Bean quản lý lấy và lưu đệm Access Token của Client Credentials flow.
   *
   * @param clientRegistrationRepository Kho lưu thông tin đăng ký Client
   * @param authorizedClientRepository Kho lưu thông tin Client đã được cấp Token
   * @return OAuth2AuthorizedClientManager instance
   */
  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager(
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientRepository authorizedClientRepository) {

    OAuth2AuthorizedClientProvider authorizedClientProvider =
        OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build();

    DefaultOAuth2AuthorizedClientManager authorizedClientManager =
        new DefaultOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientRepository);
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

    return authorizedClientManager;
  }
}
