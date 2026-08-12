package com.sso.payment.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.payment.entity.Payment;
import com.sso.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller tiếp nhận các yêu cầu thanh toán và hoàn tiền (Payment Controller).
 *
 * @author SSO Platform Team
 * @since Sprint 15
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

  private final PaymentService paymentService;

  /**
   * Yêu cầu thanh toán đơn hàng (Chỉ cho phép gọi từ internal service với scope payment:write).
   *
   * @param request Yêu cầu thanh toán chứa orderId, userId, amount, method
   * @return ResponseEntity chứa thông tin giao dịch PENDING đã khởi tạo
   */
  @PostMapping
  public ResponseEntity<ApiResponse<?>> requestPayment(@RequestBody PaymentRequest request) {
    log.info("[S2S Call] Nhận yêu cầu thanh toán cho Order ID: {}, Số tiền: {}, User: {}", 
        request.orderId(), request.amount(), request.userId());

    Payment payment = paymentService.processPayment(
        request.orderId(), 
        request.userId(), 
        request.amount(), 
        request.method()
    );

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Đã khởi tạo giao dịch thanh toán thành công", payment));
  }

  /**
   * Hoàn tiền cho giao dịch (Yêu cầu vai trò ADMIN hoặc MANAGER).
   *
   * @param id ID giao dịch thanh toán
   * @return ResponseEntity chứa thông tin giao dịch đã hoàn tiền
   */
  @PostMapping("/{id}/refund")
  public ResponseEntity<ApiResponse<?>> refundPayment(@PathVariable Long id) {
    log.info("Yêu cầu hoàn tiền cho Payment ID: {}", id);
    Payment refunded = paymentService.refundPayment(id);
    return ResponseEntity.ok(ApiResponse.success("Hoàn tiền giao dịch thành công", refunded));
  }

  /** Record DTO nhận dữ liệu đầu vào cho yêu cầu thanh toán. */
  public record PaymentRequest(
      Long orderId,
      UUID userId,
      BigDecimal amount,
      String method
  ) {}
}
