package com.sso.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ứng dụng API Gateway của hệ thống SSO Platform.
 *
 * <p>Chịu trách nhiệm định tuyến (routing), xác thực JWT tập trung, strip client headers, inject
 * trusted headers và Rate Limiting.
 *
 * @author SSO Platform Team
 * @since Sprint 11
 */
@SpringBootApplication
public class GatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }
}
