package com.sso.monolith.repository;

import com.sso.monolith.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository xử lý các truy vấn cơ sở dữ liệu cho thực thể {@link AuditLog}.
 *
 * @author SSO Platform Team
 * @since Sprint 09
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
