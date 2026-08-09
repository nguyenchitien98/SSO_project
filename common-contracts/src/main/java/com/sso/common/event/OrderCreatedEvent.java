package com.sso.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sự kiện thông báo khi một đơn hàng mới được khởi tạo thành công.
 *
 * <p>Được publish bởi order-service sử dụng Outbox Pattern.
 * Được consume bởi:
 * <ul>
 *   <li>payment-service: Để tự động tạo giao dịch chờ thanh toán.</li>
 *   <li>product-service: Để tạm giữ/trừ số lượng tồn kho (stock) của sản phẩm.</li>
 *   <li>notification-service: Gửi email xác nhận đơn hàng cho khách hàng.</li>
 * </ul>
 *
 * @param eventId Định danh sự kiện duy nhất (UUID)
 * @param orderId Định danh đơn hàng (Order ID)
 * @param orderCode Mã hiển thị đơn hàng (ví dụ: ORD-2026-xxxx)
 * @param userId Định danh khách hàng mua (SSO User UUID)
 * @param totalAmount Tổng tiền đơn hàng
 * @param idempotencyKey Key chống trùng lặp đơn hàng
 * @param items Danh sách các sản phẩm trong đơn hàng
 * @param createdAt Thời điểm tạo sự kiện
 * @author SSO Platform Team
 * @since Sprint 01
 */
public record OrderCreatedEvent(
        UUID eventId,
        Long orderId,
        String orderCode,
        UUID userId,
        BigDecimal totalAmount,
        String idempotencyKey,
        List<OrderItem> items,
        Instant createdAt
) {
    /**
     * Đại diện cho một mặt hàng trong sự kiện đặt hàng.
     *
     * @param productId ID sản phẩm
     * @param productName Tên sản phẩm tại thời điểm đặt
     * @param quantity Số lượng đặt mua
     * @param unitPrice Đơn giá sản phẩm
     */
    public record OrderItem(
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice
    ) {}
}
