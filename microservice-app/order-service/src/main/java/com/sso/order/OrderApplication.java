package com.sso.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Lớp chạy chính của Order Service.
 *
 * @author SSO Platform Team
 * @since Sprint 12
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    FlywayAutoConfiguration.class
})
@EnableDiscoveryClient
public class OrderApplication {

  public static void main(String[] args) {
    SpringApplication.run(OrderApplication.class, args);
  }
}
