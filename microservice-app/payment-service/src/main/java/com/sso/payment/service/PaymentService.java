package com.sso.payment.service;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.payment.entity.Payment;
import com.sso.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp dịch vụ quản lý các giao dịch thanh toán (Payment Service).
 *
 * @author SSO Platform Team
 * @since Sprint 15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final com.sso.payment.repository.OutboxEventRepository outboxRepository;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  /**
   * Tạo giao dịch thanh toán mới ở trạng thái PENDING, giả lập bất đồng bộ chuyển COMPLETED sau 2 giây.
   *
   * @param orderId ID đơn hàng
   * @param userId UUID người dùng mua hàng
   * @param amount Số tiền thanh toán
   * @param method Phương thức thanh toán (ví dụ: E-WALLET)
   * @return Đối tượng Payment đã tạo
   */
  @Transactional
  public Payment processPayment(Long orderId, UUID userId, BigDecimal amount, String method) {
    log.info("Bắt đầu xử lý thanh toán cho đơn hàng ID: {}, Số tiền: {}, User: {}", orderId, amount, userId);

    Payment payment = Payment.builder()
        .orderId(orderId)
        .userId(userId)
        .amount(amount)
        .method(method)
        .status("PENDING")
        .transactionRef("TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
        .createdAt(Instant.now())
        .build();

    Payment savedPayment = paymentRepository.save(payment);

    // Giả lập xử lý thanh toán bất đồng bộ chuyển sang trạng thái COMPLETED sau 2 giây
    simulateAsyncPaymentCompletion(savedPayment.getId());

    return savedPayment;
  }

  /**
   * Hoàn tiền cho giao dịch thanh toán (Chỉ cho phép khi ở trạng thái COMPLETED).
   *
   * @param paymentId ID giao dịch thanh toán
   * @return Đối tượng Payment đã được hoàn tiền (REFUNDED)
   */
  @Transactional
  public Payment refundPayment(Long paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy giao dịch thanh toán"));

    if (!"COMPLETED".equals(payment.getStatus())) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT, "Chỉ có thể hoàn tiền cho giao dịch đã hoàn tất (Trạng thái hiện tại: " + payment.getStatus() + ")");
    }

    payment.setStatus("REFUNDED");
    return paymentRepository.save(payment);
  }

  @Transactional(readOnly = true)
  public Payment getPaymentById(Long paymentId) {
    return paymentRepository.findById(paymentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy giao dịch thanh toán"));
  }

  private void simulateAsyncPaymentCompletion(Long paymentId) {
    CompletableFuture.runAsync(() -> {
      try {
        log.info("[Async Payment] Đang giả lập xử lý thanh toán ngân hàng (chờ 2 giây) cho Payment ID: {}...", paymentId);
        Thread.sleep(2000);
        
        // Cập nhật trạng thái Payment sang COMPLETED
        updatePaymentStatus(paymentId, "COMPLETED");
        log.info("[Async Payment] Thanh toán thành công (COMPLETED) cho Payment ID: {}", paymentId);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("[Async Payment] Tiến trình giả lập bị ngắt quãng", e);
      } catch (Exception e) {
        log.error("[Async Payment] Lỗi khi cập nhật trạng thái thanh toán", e);
      }
    });
  }

  @Transactional
  public void updatePaymentStatus(Long paymentId, String status) {
    paymentRepository.findById(paymentId).ifPresent(payment -> {
      payment.setStatus(status);
      Payment savedPayment = paymentRepository.save(payment);

      if ("COMPLETED".equals(status)) {
        try {
          com.sso.common.event.PaymentCompletedEvent eventPayload = new com.sso.common.event.PaymentCompletedEvent(
              UUID.randomUUID(),
              savedPayment.getId(),
              savedPayment.getOrderId(),
              savedPayment.getUserId(),
              savedPayment.getAmount(),
              status,
              savedPayment.getTransactionRef(),
              Instant.now()
          );

          com.sso.payment.entity.OutboxEvent outboxEvent = com.sso.payment.entity.OutboxEvent.builder()
              .eventType("PAYMENT_COMPLETED")
              .aggregateId(savedPayment.getId().toString())
              .payload(objectMapper.writeValueAsString(eventPayload))
              .status("PENDING")
              .createdAt(Instant.now())
              .build();

          outboxRepository.save(outboxEvent);
          log.info("[Outbox] Đã lưu sự kiện PAYMENT_COMPLETED cho giao dịch ID: {}", savedPayment.getId());
        } catch (Exception e) {
          log.error("[Outbox] Lỗi khi tạo sự kiện PAYMENT_COMPLETED outbox: ", e);
        }
      }
    });
  }
}
