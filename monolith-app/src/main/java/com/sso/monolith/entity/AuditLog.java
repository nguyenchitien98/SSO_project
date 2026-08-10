package com.sso.monolith.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Thực thể Đại diện cho Bản ghi nhật ký lịch sử thao tác (Audit Log) lưu trong cơ sở dữ liệu.
 *
 * <p>Ánh xạ với bảng `audit_logs` trong CSDL `monolith_db`.
 *
 * @author SSO Platform Team
 * @since Sprint 09
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_id", nullable = false)
  private UserProfile actor;

  @Column(name = "actor_email", length = 255)
  private String actorEmail;

  @Column(nullable = false, length = 100)
  private String action;

  @Column(name = "entity_type", nullable = false, length = 50)
  private String entityType;

  @Column(name = "entity_id", length = 100)
  private String entityId;

  @Column(name = "old_values", columnDefinition = "jsonb")
  private String oldValues;

  @Column(name = "new_values", columnDefinition = "jsonb")
  private String newValues;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
  }
}
