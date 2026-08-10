package com.sso.server.security;

import com.sso.server.entity.OauthClient;
import com.sso.server.repository.OauthClientRepository;
import java.time.Duration;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

/**
 * Cài đặt custom {@link RegisteredClientRepository} kết nối PostgreSQL thông qua JPA.
 *
 * <p>Thay vì lưu trữ danh sách Clients OAuth2 trong bộ nhớ tạm (In-Memory), lớp này truy vấn bảng
 * `oauth_clients` để nạp cấu hình client động tại runtime.
 *
 * <p>Quy trình map cấu hình: - Ánh xạ các trường dữ liệu `grant_types`, `redirect_uris`, `scopes`
 * dạng chuỗi thô phân cách bằng dấu phẩy/khoảng trắng thành các đối tượng tương ứng của Spring
 * Security. - Cấu hình thời gian sống của token: Access Token (mặc định 15 phút), Refresh Token
 * (mặc định 7 ngày). - Bật/tắt yêu cầu xác thực mã PKCE và hiển thị màn hình chấp thuận (consent).
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

  private final OauthClientRepository oauthClientRepository;

  @Override
  public void save(RegisteredClient registeredClient) {
    log.warn(
        "Thao tác save client trực tiếp bị vô hiệu hóa qua Repository này. Vui lòng cập nhật DB qua Seed hoặc Admin API.");
  }

  @Override
  public RegisteredClient findById(String id) {
    log.info("Truy vấn thông tin OAuth2 Client theo ID: {}", id);
    return oauthClientRepository.findById(id).map(this::mapToRegisteredClient).orElse(null);
  }

  @Override
  public RegisteredClient findByClientId(String clientId) {
    log.info("Truy vấn thông tin OAuth2 Client theo Client ID: {}", clientId);
    return oauthClientRepository.findById(clientId).map(this::mapToRegisteredClient).orElse(null);
  }

  private RegisteredClient mapToRegisteredClient(OauthClient client) {
    RegisteredClient.Builder builder =
        RegisteredClient.withId(client.getId())
            .clientId(client.getId())
            .clientSecret(client.getClientSecret())
            .clientName(client.getClientName());

    // Cấu hình các phương thức Client Authentication được chấp nhận
    builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
    builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
    builder.clientAuthenticationMethod(
        ClientAuthenticationMethod.NONE); // Dành cho PKCE Public Client

    // Map các Grant Types
    if (client.getGrantTypes() != null) {
      Arrays.stream(client.getGrantTypes().split(","))
          .map(String::trim)
          .forEach(gt -> builder.authorizationGrantType(new AuthorizationGrantType(gt)));
    }

    // Map các Redirect URIs
    if (client.getRedirectUris() != null) {
      Arrays.stream(client.getRedirectUris().split(","))
          .map(String::trim)
          .forEach(builder::redirectUri);
    }

    // Map các Scopes
    if (client.getScopes() != null) {
      Arrays.stream(client.getScopes().split(" ")).map(String::trim).forEach(builder::scope);
    }

    // Cấu hình Token TTL
    builder.tokenSettings(
        TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofSeconds(client.getAccessTokenTtlSeconds()))
            .refreshTokenTimeToLive(Duration.ofSeconds(client.getRefreshTokenTtlSeconds()))
            .reuseRefreshTokens(false) // Issuing new refresh token each time rotated
            .build());

    // Cấu hình Client Settings (PKCE & Consent & Back-Channel Logout)
    ClientSettings.Builder clientSettingsBuilder =
        ClientSettings.builder()
            .requireProofKey(client.isRequirePkce())
            .requireAuthorizationConsent(client.isRequireAuthorizationConsent());

    if (client.getBackChannelLogoutUri() != null && !client.getBackChannelLogoutUri().isBlank()) {
      clientSettingsBuilder.setting(
          "settings.client.oidc.back-channel-logout-uri", client.getBackChannelLogoutUri());
    }
    builder.clientSettings(clientSettingsBuilder.build());

    return builder.build();
  }
}
