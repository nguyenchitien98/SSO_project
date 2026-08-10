package com.sso.monolith.repository;

import com.sso.monolith.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository xử lý các truy vấn cơ sở dữ liệu cho thực thể {@link Payment}.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {}
