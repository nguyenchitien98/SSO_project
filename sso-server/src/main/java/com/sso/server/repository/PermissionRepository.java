package com.sso.server.repository;

import com.sso.server.entity.Permission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Kho lưu trữ dữ liệu (Repository) cho thực thể {@link Permission}.
 *
 * <p>Cung cấp các phương thức truy vấn cơ sở dữ liệu cho bảng `permissions`.
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

  /**
   * Tìm kiếm quyền hạn dựa trên tên định danh (ví dụ: 'PRODUCT_READ').
   *
   * @param name Tên quyền hạn cần tìm
   * @return Permission bọc trong Optional
   */
  Optional<Permission> findByName(String name);
}
