package com.sso.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sso.gateway.filter.InjectTrustedHeadersFilter;
import com.sso.gateway.filter.StripClientHeadersFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Lớp kiểm thử tích hợp (Integration Test) cho cơ chế bảo mật trên API Gateway.
 *
 * @author SSO Platform Team
 * @since Sprint 11
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class GatewaySecurityIntegrationTest {

  @Autowired private WebTestClient webTestClient;

  @Autowired private StripClientHeadersFilter stripClientHeadersFilter;

  @Autowired private InjectTrustedHeadersFilter injectTrustedHeadersFilter;

  @Test
  public void contextLoads() {
    // Đảm bảo Spring context load thành công và các filter được khởi tạo
    assertThat(stripClientHeadersFilter).isNotNull();
    assertThat(injectTrustedHeadersFilter).isNotNull();
  }

  @Test
  public void testPublicEndpoint_HealthCheck_ReturnsSuccess() {
    // Endpoint công khai /actuator/health không yêu cầu JWT
    webTestClient
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  public void testSecureEndpoint_WithoutJwt_ReturnsUnauthorized() {
    // Endpoint nghiệp vụ bảo mật /api/products/** bắt buộc mang JWT
    webTestClient
        .get()
        .uri("/api/products")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
