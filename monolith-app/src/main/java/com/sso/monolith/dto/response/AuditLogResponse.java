package com.sso.monolith.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * DTO phản hồi thông tin nhật ký thao tác (Audit Log) cho giao diện quản trị.
 *
 * @author SSO Platform Team
 * @since Sprint 10.5
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
  private Long id;
  private UUID actorId;
  private String actorName;
  private String actorEmail;
  private String action;
  private String entityType;
  private String entityId;
  private String ipAddress;
  private Instant createdAt;
}
