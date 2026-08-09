package com.sso.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lớp khởi chạy chính (Main Class) cho ứng dụng SSO Authorization Server.
 *
 * <p>SSO Server đóng vai trò là Identity Provider (IdP) trung tâm của toàn bộ hệ thống,
 * thực hiện phát hành ID Token, Access Token (JWT) và Refresh Token theo các chuẩn OAuth2
 * và OpenID Connect (OIDC).
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@SpringBootApplication
public class SsoServerApplication {

    /**
     * Phương thức main để chạy ứng dụng Spring Boot.
     *
     * @param args Các tham số đầu vào dòng lệnh
     */
    public static void main(String[] args) {
        SpringApplication.run(SsoServerApplication.class, args);
    }
}
