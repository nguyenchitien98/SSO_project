package com.sso.monolith.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.monolith.dto.response.AuditLogResponse;
import com.sso.monolith.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp API quản lý nhật ký thao tác (Audit Logs) cho quản trị viên.
 *
 * @author SSO Platform Team
 * @since Sprint 10.5
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

  private final AuditLogRepository auditLogRepository;

  /**
   * Lấy danh sách audit logs phân trang.
   *
   * <p>Yêu cầu quyền hạn: `AUDIT_READ`.
   */
  @GetMapping
  @PreAuthorize("hasAuthority('AUDIT_READ')")
  public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(Pageable pageable) {
    log.info("API GET /api/admin/audit-logs - Truy vấn danh sách nhật ký phân trang");

    Page<AuditLogResponse> response =
        auditLogRepository
            .findAll(pageable)
            .map(
                logEntity ->
                    AuditLogResponse.builder()
                        .id(logEntity.getId())
                        .actorId(logEntity.getActor() != null ? logEntity.getActor().getId() : null)
                        .actorName(
                            logEntity.getActor() != null
                                ? logEntity.getActor().getDisplayName()
                                : "Hệ thống")
                        .actorEmail(logEntity.getActorEmail())
                        .action(logEntity.getAction())
                        .entityType(logEntity.getEntityType())
                        .entityId(logEntity.getEntityId())
                        .ipAddress(logEntity.getIpAddress())
                        .createdAt(logEntity.getCreatedAt())
                        .build());

    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
