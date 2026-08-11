package com.sso.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Ứng dụng Khám phá Dịch vụ (Spring Cloud Netflix Eureka Server).
 *
 * <p>Quản lý việc đăng ký thông tin trạng thái hoạt động (Service Registry) của tất cả các
 * instance microservices trong hệ thống.
 *
 * @author SSO Platform Team
 * @since Sprint 12
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(EurekaServerApplication.class, args);
  }
}
