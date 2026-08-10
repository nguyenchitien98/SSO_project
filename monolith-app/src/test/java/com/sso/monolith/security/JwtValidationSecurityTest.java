package com.sso.monolith.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Kiểm thử tích hợp bảo mật các kịch bản tấn công Token JWT (Jwt Authentication Security Test).
 *
 * <p>Kiểm chứng rằng bộ phân giải Resource Server của Spring Security sẽ reject tất cả token không
 * hợp lệ: - Token bị sửa đổi chữ ký (Tampered JWT). - Token giả mạo thuật toán không chữ ký (alg:
 * none). - Token có Issuer hoặc Audience sai lệch. - Token đã hết hạn sử dụng.
 *
 * @author SSO Platform Team
 * @since Sprint 09
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtValidationSecurityTest {

  @Autowired private MockMvc mockMvc;

  @MockBean
  private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
      clientRegistrationRepository;

  @MockBean private JwtDecoder jwtDecoder;

  /** Test: Gửi token JWT bị tampered chữ ký ➔ Phải trả về HTTP 401. */
  @Test
  void requestWithTamperedJwt_Returns401() throws Exception {
    when(jwtDecoder.decode("tampered-token"))
        .thenThrow(new BadJwtException("Signature validation failed"));

    mockMvc
        .perform(get("/api/products").header("Authorization", "Bearer tampered-token"))
        .andExpect(status().isUnauthorized());
  }

  /** Test: Gửi token JWT giả mạo thuật toán alg:none ➔ Phải trả về HTTP 401. */
  @Test
  void requestWithAlgNoneJwt_Returns401() throws Exception {
    when(jwtDecoder.decode("alg-none-token"))
        .thenThrow(new BadJwtException("Algorithm 'none' is not allowed"));

    mockMvc
        .perform(get("/api/products").header("Authorization", "Bearer alg-none-token"))
        .andExpect(status().isUnauthorized());
  }

  /** Test: Gửi token JWT có Issuer hoặc Audience sai lệch ➔ Phải trả về HTTP 401. */
  @Test
  void requestWithInvalidIssuerOrAudienceJwt_Returns401() throws Exception {
    OAuth2Error error = new OAuth2Error("invalid_token", "The iss claim is not valid", null);
    when(jwtDecoder.decode("invalid-issuer-token"))
        .thenThrow(new JwtValidationException("Invalid token claims", List.of(error)));

    mockMvc
        .perform(get("/api/products").header("Authorization", "Bearer invalid-issuer-token"))
        .andExpect(status().isUnauthorized());
  }

  /** Test: Gửi token JWT đã hết hạn (Expired Token) ➔ Phải trả về HTTP 401. */
  @Test
  void requestWithExpiredJwt_Returns401() throws Exception {
    OAuth2Error error = new OAuth2Error("invalid_token", "Jwt is expired", null);
    when(jwtDecoder.decode("expired-token"))
        .thenThrow(new JwtValidationException("Jwt is expired", List.of(error)));

    mockMvc
        .perform(get("/api/products").header("Authorization", "Bearer expired-token"))
        .andExpect(status().isUnauthorized());
  }
}
