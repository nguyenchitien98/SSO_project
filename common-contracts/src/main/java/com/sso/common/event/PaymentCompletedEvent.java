package com.sso.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Sự kiện thông báo khi giao dịch thanh toán của đơn hàng được xử lý hoàn tất.
 *
 * <p>Được publish bởi payment-service sử dụng Outbox Pattern.
 * Được consume bởi:
 * <ul>
 *   <li>order-service: Để chuyển trạng thái đơn hàng từ PENDING sang CONFIRMED.</li>
 *   <li>notification-service: Gửi thông báo thanh toán thành công/hóa đơn cho khách hàng.</li>
 * </ul>
 *
 * @param eventId Định danh sự kiện duy nhất (UUID)
 * @param paymentId Định danh bản ghi thanh toán
 * @param orderId Định danh đơn hàng liên kết
 * @param userId Định danh người dùng thanh toán
 * @param amount Số tiền đã thanh toán
 * @param status Trạng thái thanh toán (COMPLETED, FAILED, REFUNDED)
 * @param transactionRef Mã tham chiếu giao dịch từ cổng thanh toán đối tác
 * @param completedAt Thời điểm hoàn thành thanh toán
 * @author SSO Platform Team
 * @since Sprint 01
 */
public record PaymentCompletedEvent(
        UUID eventId,
        Long paymentId,
        Long orderId,
        UUID userId,
        BigDecimal amount,
        String status,
        String transactionRef,
        Instant completedAt
) {}
