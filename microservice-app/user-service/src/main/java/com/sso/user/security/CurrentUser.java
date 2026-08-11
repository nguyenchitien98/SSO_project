package com.sso.user.security;

import java.util.List;

/**
 * Record lưu trữ thông tin của người dùng hiện tại đang gửi yêu cầu.
 *
 * <p>Được giải mã từ HTTP trusted headers truyền từ API Gateway.
 *
 * @author SSO Platform Team
 * @since Sprint 12
 */
public record CurrentUser(
    String id,
    String email,
    List<String> roles,
    List<String> permissions
) {}
