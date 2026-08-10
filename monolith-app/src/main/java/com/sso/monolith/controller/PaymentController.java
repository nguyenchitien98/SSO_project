package com.sso.monolith.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.monolith.dto.request.ProcessPaymentRequest;
import com.sso.monolith.dto.response.PaymentResponse;
import com.sso.monolith.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp API xử lý thanh toán (giả lập sandbox) trên Monolith.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

  private final PaymentService paymentService;

  /** API thực hiện thanh toán đơn hàng. */
  @PostMapping
  public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
      @Valid @RequestBody ProcessPaymentRequest request) {

    log.info(
        "API POST /api/payments - Yêu cầu xử lý thanh toán đơn hàng ID: {}", request.getOrderId());
    PaymentResponse response = paymentService.processPayment(request);
    return ResponseEntity.ok(ApiResponse.success("Thanh toán thành công đơn hàng", response));
  }
}
