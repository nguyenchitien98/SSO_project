package com.sso.monolith.service;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.monolith.dto.request.ProcessPaymentRequest;
import com.sso.monolith.dto.response.PaymentResponse;
import com.sso.monolith.entity.Order;
import com.sso.monolith.entity.Payment;
import com.sso.monolith.repository.OrderRepository;
import com.sso.monolith.repository.PaymentRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp dịch vụ (Service) xử lý các nghiệp vụ liên quan đến Thanh toán (Payment).
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;

  /**
   * Xử lý thanh toán đơn hàng (Giả lập Sandbox).
   *
   * <p>Nghiệp vụ bao gồm: - Tìm kiếm đơn hàng theo orderId. - Lưu lịch sử thanh toán thành công. -
   * Cập nhật trạng thái đơn hàng sang "COMPLETED".
   *
   * @param request DTO yêu cầu thanh toán
   * @return DTO phản hồi kết quả thanh toán
   */
  @Transactional
  @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
  public PaymentResponse processPayment(ProcessPaymentRequest request) {
    log.info(
        "Bắt đầu xử lý thanh toán cho đơn hàng ID: {}, Số tiền: {}",
        request.getOrderId(),
        request.getAmount());

    Order order =
        orderRepository
            .findById(request.getOrderId())
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "Không tìm thấy đơn hàng để thanh toán: ID " + request.getOrderId()));

    if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT,
          "Đơn hàng đã được xử lý trước đó. Trạng thái hiện tại: " + order.getStatus());
    }

    // Tạo thanh toán giả lập Sandbox thành công
    Payment payment =
        Payment.builder()
            .order(order)
            .amount(request.getAmount())
            .method(request.getMethod())
            .status("SUCCESS")
            .transactionRef("TXN-" + System.currentTimeMillis())
            .build();

    Payment savedPayment = paymentRepository.save(payment);

    // Cập nhật trạng thái đơn hàng sang COMPLETED
    order.setStatus("COMPLETED");
    order.setUpdatedAt(Instant.now());
    orderRepository.save(order);

    log.info(
        "Thanh toán thành công đơn hàng ID: {}, Mã giao dịch: {}",
        order.getId(),
        savedPayment.getTransactionRef());

    return PaymentResponse.builder()
        .id(savedPayment.getId())
        .orderId(order.getId())
        .amount(savedPayment.getAmount())
        .method(savedPayment.getMethod())
        .status(savedPayment.getStatus())
        .transactionRef(savedPayment.getTransactionRef())
        .createdAt(savedPayment.getCreatedAt())
        .build();
  }
}
