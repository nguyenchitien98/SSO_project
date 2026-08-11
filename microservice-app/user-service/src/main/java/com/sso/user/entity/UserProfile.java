package com.sso.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Thực thể lưu trữ hồ sơ chi tiết người dùng (User Profile Entity).
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id; // Trùng với Subject/UUID của SSO Server

  @Column(name = "display_name", length = 100)
  private String displayName;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Column(name = "phone", length = 20)
  private String phone;

  @Column(name = "bio", columnDefinition = "TEXT")
  private String bio;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "preferences", columnDefinition = "jsonb")
  @Builder.Default
  private Map<String, Object> preferences = new HashMap<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  @Builder.Default
  private Instant updatedAt = Instant.now();
}
