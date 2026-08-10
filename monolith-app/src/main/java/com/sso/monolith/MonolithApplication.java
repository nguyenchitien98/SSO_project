package com.sso.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lớp khởi động (Bootstrap Application) của ứng dụng Spring Boot Monolith.
 *
 * <p>Tích hợp các thành phần nghiệp vụ cốt lõi, cơ sở dữ liệu Postgres chéo của Monolith và bảo mật
 * chéo xác thực thông qua SSO Server sử dụng OAuth2 Login và Resource Server.
 *
 * @author SSO Platform Team
 * @since Sprint 06
 */
@SpringBootApplication
public class MonolithApplication {

  public static void main(String[] args) {
    SpringApplication.run(MonolithApplication.class, args);
  }
}
