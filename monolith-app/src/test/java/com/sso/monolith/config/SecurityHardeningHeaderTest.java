package com.sso.monolith.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Kiểm thử tích hợp bảo mật (Security Integration Test) cho các Header bảo mật (Security Headers).
 *
 * <p>Kiểm chứng các headers bảo mật nâng cao chống tấn công mạng đã được cấu hình chính xác: -
 * `X-Frame-Options: DENY` (Clickjacking). - `X-Content-Type-Options: nosniff` (MIME sniffing). -
 * `Content-Security-Policy` (CSP). - `Strict-Transport-Security` (HSTS).
 *
 * @author SSO Platform Team
 * @since Sprint 09
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningHeaderTest {

  @Autowired private MockMvc mockMvc;

  @MockBean
  private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
      clientRegistrationRepository;

  @MockBean private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

  @Test
  void getActuatorHealth_VerifySecurityHeaders() throws Exception {
    mockMvc
        .perform(get("/actuator/health").secure(true))
        .andExpect(
            result -> {
              int status = result.getResponse().getStatus();
              org.junit.jupiter.api.Assertions.assertTrue(status == 200 || status == 503);
            })
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Content-Security-Policy", "default-src 'self'"))
        .andExpect(
            header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"));
  }
}
