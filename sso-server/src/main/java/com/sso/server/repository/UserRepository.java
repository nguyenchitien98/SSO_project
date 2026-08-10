package com.sso.server.repository;

import com.sso.server.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Kho lưu trữ dữ liệu (Repository) cho thực thể {@link User}.
 *
 * <p>Cung cấp các phương thức truy vấn cơ sở dữ liệu cho bảng `users`.
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  /**
   * Tìm kiếm người dùng dựa trên username đăng nhập.
   *
   * @param username Tên đăng nhập cần tìm
   * @return User tương ứng bọc trong Optional
   */
  Optional<User> findByUsername(String username);

  /**
   * Tìm kiếm người dùng dựa trên email đăng ký.
   *
   * @param email Địa chỉ email cần tìm
   * @return User tương ứng bọc trong Optional
   */
  Optional<User> findByEmail(String email);
}
