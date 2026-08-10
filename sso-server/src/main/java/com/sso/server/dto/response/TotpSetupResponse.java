package com.sso.server.dto.response;

import lombok.*;

/**
 * DTO chứa thông tin cài đặt xác thực 2 lớp (TOTP 2FA).
 *
 * @author SSO Platform Team
 * @since Sprint 04
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotpSetupResponse {
  private String secretKey;
  private String qrCodeUrl;
}
