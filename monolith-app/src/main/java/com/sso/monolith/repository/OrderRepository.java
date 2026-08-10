package com.sso.monolith.repository;

import com.sso.monolith.entity.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository xử lý các truy vấn cơ sở dữ liệu cho thực thể {@link Order}.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  /** Tìm kiếm danh sách đơn hàng thuộc một người dùng cụ thể phân trang. */
  Page<Order> findByUser_Id(UUID userId, Pageable pageable);

  /** Tìm kiếm đơn hàng theo mã đơn hàng duy nhất. */
  Optional<Order> findByOrderCode(String orderCode);

  /** Kiểm tra sự tồn tại của Idempotency Key để tránh xử lý trùng lặp đơn hàng. */
  boolean existsByIdempotencyKey(String idempotencyKey);
}
