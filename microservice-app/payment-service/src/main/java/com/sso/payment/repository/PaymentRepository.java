package com.sso.payment.repository;

import com.sso.payment.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository quản lý thực thể Payment.
 *
 * @author SSO Platform Team
 * @since Sprint 15
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
  Optional<Payment> findByOrderId(Long orderId);
}
