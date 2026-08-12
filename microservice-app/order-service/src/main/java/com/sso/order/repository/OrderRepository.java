package com.sso.order.repository;

import com.sso.order.entity.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository quản lý thực thể Order.
 *
 * @author SSO Platform Team
 * @since Sprint 14
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  Page<Order> findAllByUserId(UUID userId, Pageable pageable);

  Optional<Order> findByOrderCode(String orderCode);

  @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID'")
  java.math.BigDecimal sumTotalRevenue();
}
