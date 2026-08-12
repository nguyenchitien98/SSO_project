package com.sso.order.client;

import com.sso.common.dto.ApiResponse;
import com.sso.order.dto.PaymentRequestDto;
import com.sso.order.dto.PaymentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign Client liên kết tới Payment Service để thực hiện thanh toán đơn hàng.
 *
 * @author SSO Platform Team
 * @since Sprint 15
 */
@FeignClient(name = "payment-service", path = "/api/payments")
public interface PaymentClient {

  @PostMapping
  ApiResponse<PaymentResponseDto> requestPayment(@RequestBody PaymentRequestDto request);
}
