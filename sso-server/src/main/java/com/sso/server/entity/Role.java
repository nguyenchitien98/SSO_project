package com.sso.server.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

/**
 * Đại diện cho vai trò (Role) của người dùng trong hệ thống phân quyền (RBAC).
 *
 * <p>Ánh xạ với bảng `roles` trong cơ sở dữ liệu `sso_db`.
 * Một vai trò có thể liên kết với nhiều quyền (Permissions) khác nhau
 * thông qua bảng trung gian `role_permissions`.
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
