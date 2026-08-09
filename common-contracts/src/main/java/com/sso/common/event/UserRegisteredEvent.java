package com.sso.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Sự kiện thông báo khi có người dùng mới đăng ký thành công trên SSO Server.
 *
 * <p>Được publish bởi SSO Server khi hoàn tất tạo tài khoản.
 * Được consume bởi:
 * <ul>
 *   <li>user-service: Để tự động tạo hồ sơ UserProfile rỗng của người dùng.</li>
 *   <li>notification-service: Để gửi email chào mừng/kích hoạt tài khoản.</li>
 * </ul>
 *
 * @param id Định danh duy nhất của sự kiện (UUID)
 * @param userId Định danh người dùng (SSO User UUID)
 * @param username Tên đăng nhập của người dùng
 * @param email Địa chỉ email của người dùng
 * @param createdAt Thời điểm sự kiện xảy ra
 * @author SSO Platform Team
 * @since Sprint 01
 */
public record UserRegisteredEvent(
        UUID id,
        UUID userId,
        String username,
        String email,
        Instant createdAt
) {
    /**
     * Khởi tạo nhanh sự kiện với ID ngẫu nhiên và thời gian hiện tại.
     *
     * @param userId Định danh người dùng
     * @param username Tên đăng nhập
     * @param email Địa chỉ email
     * @return Đối tượng UserRegisteredEvent
     */
    public static UserRegisteredEvent of(UUID userId, String username, String email) {
        return new UserRegisteredEvent(UUID.randomUUID(), userId, username, email, Instant.now());
    }
}
