package com.sso.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Đại diện cho cấu hình của một Client OAuth2 đăng ký trong hệ thống.
 *
 * <p>Ánh xạ với bảng `oauth_clients` trong cơ sở dữ liệu `sso_db`.
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Entity
@Table(name = "oauth_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OauthClient {

  @Id private String id;

  @Column(name = "client_secret", length = 255)
  private String clientSecret;

  @Column(name = "client_name", nullable = false, length = 100)
  private String clientName;

  @Column(name = "grant_types", nullable = false, columnDefinition = "TEXT")
  private String grantTypes;

  @Column(name = "redirect_uris", columnDefinition = "TEXT")
  private String redirectUris;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String scopes;

  @Builder.Default
  @Column(name = "access_token_ttl_seconds", nullable = false)
  private int accessTokenTtlSeconds = 900; // 15 phút

  @Builder.Default
  @Column(name = "refresh_token_ttl_seconds", nullable = false)
  private int refreshTokenTtlSeconds = 604800; // 7 ngày

  @Builder.Default
  @Column(name = "require_pkce", nullable = false)
  private boolean requirePkce = true;

  @Builder.Default
  @Column(name = "require_authorization_consent", nullable = false)
  private boolean requireAuthorizationConsent = false;
}
