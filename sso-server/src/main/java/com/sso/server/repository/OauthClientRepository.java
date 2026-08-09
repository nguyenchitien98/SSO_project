package com.sso.server.repository;

import com.sso.server.entity.OauthClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Kho lưu trữ dữ liệu (Repository) cho thực thể {@link OauthClient}.
 *
 * <p>Cung cấp các phương thức truy vấn cấu hình Client OAuth2 từ bảng `oauth_clients`.
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Repository
public interface OauthClientRepository extends JpaRepository<OauthClient, String> {
}
