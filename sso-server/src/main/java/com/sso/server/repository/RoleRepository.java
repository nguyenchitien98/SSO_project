package com.sso.server.repository;

import com.sso.server.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Kho lưu trữ dữ liệu (Repository) cho thực thể {@link Role}.
 *
 * <p>Cung cấp các phương thức truy vấn cơ sở dữ liệu cho bảng `roles`.
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  /**
   * Tìm kiếm vai trò theo tên vai trò (ví dụ: 'ADMIN', 'USER').
   *
   * @param name Tên vai trò cần tìm
   * @return Role bọc trong Optional
   */
  Optional<Role> findByName(String name);
}
