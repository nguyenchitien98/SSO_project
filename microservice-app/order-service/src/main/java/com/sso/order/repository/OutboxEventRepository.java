package com.sso.order.repository;

import com.sso.order.entity.OutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository quản lý thực thể OutboxEvent.
 *
 * @author SSO Platform Team
 * @since Sprint 16
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

  List<OutboxEvent> findAllByStatusOrderByCreatedAtAsc(String status);
}
