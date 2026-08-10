package com.sso.monolith.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Thực thể lưu trữ hồ sơ chi tiết người dùng cục bộ tại Monolith App.
 *
 * <p>Tại sao khóa chính là UUID tự gán? - `id` ở đây liên kết chéo 1-1 với ID tài khoản của người
 * dùng lưu trên SSO Server. - Khi xác thực thành công qua JWT, Monolith chỉ cần đọc `sub` claim để
 * truy vấn profile. - Thiết kế này giúp hệ thống vừa phân tách dữ liệu (SSO quản lý bảo mật,
 * Monolith quản lý nghiệp vụ) vừa đồng bộ danh tính hoàn hảo.
 *
 * @author SSO Platform Team
 * @since Sprint 06
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

  @Id private UUID id;

  @Column(name = "display_name", length = 100)
  private String displayName;

  @Column(length = 20)
  private String phone;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Column(columnDefinition = "TEXT")
  private String address;

  @Column(columnDefinition = "jsonb")
  private String preferences;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
    if (this.preferences == null) {
      this.preferences = "{}";
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
