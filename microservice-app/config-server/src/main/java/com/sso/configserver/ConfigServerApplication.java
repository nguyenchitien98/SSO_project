package com.sso.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Ứng dụng Cấu hình Tập trung (Spring Cloud Config Server).
 *
 * <p>Quản lý toàn bộ cấu hình của các microservice con trong hệ thống tại local classpath resources.
 *
 * @author SSO Platform Team
 * @since Sprint 12
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigServerApplication.class, args);
  }
}
