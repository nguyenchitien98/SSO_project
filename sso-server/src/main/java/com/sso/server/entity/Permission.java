package com.sso.server.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Đại diện cho một quyền cụ thể (Permission) trong hệ thống phân quyền (RBAC).
 *
 * <p>Ánh xạ với bảng `permissions` trong cơ sở dữ liệu `sso_db`.
 * Mỗi permission được định nghĩa dựa trên resource tác động và hành động tương ứng
 * (ví dụ: PRODUCT_CREATE tương ứng với resource="PRODUCT", action="CREATE").
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 50)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
