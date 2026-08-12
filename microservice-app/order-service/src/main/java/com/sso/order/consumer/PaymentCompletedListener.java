package com.sso.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sso.common.event.PaymentCompletedEvent;
import com.sso.order.entity.Order;
import com.sso.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lắng nghe sự kiện hoàn thành thanh toán từ Kafka để cập nhật trạng thái đơn hàng.
 *
 * @author SSO Platform Team
 * @since Sprint 16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedListener {

  private final OrderRepository orderRepository;
  private final ObjectMapper objectMapper;

  /**
   * Lắng nghe thông điệp trên topic payment-completed.
   *
   * @param message Nội dung thông điệp dạng JSON String
   */
  @KafkaListener(topics = "payment-completed", groupId = "order-group")
  @Transactional
  public void consumePaymentCompleted(String message) {
    try {
      log.info("[Kafka Consumer] Nhận được sự kiện thanh toán: {}", message);
      PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);

      Order order = orderRepository.findById(event.orderId())
          .orElse(null);

      if (order == null) {
        log.warn("[Kafka Consumer] Không tìm thấy đơn hàng ID: {} để xác thực thanh toán!", event.orderId());
        return;
      }

      // Kiểm tra tính trùng lặp / idempotency ở mức database
      if ("PAID".equals(order.getStatus())) {
        log.info("[Kafka Consumer] Đơn hàng ID: {} đã ở trạng thái PAID. Bỏ qua sự kiện.", order.getId());
        return;
      }

      if ("COMPLETED".equals(event.status())) {
        order.setStatus("PAID");
        orderRepository.save(order);
        log.info("[Kafka Consumer] Cập nhật đơn hàng ID: {} sang trạng thái PAID thành công.", order.getId());
      } else if ("FAILED".equals(event.status())) {
        order.setStatus("FAILED");
        orderRepository.save(order);
        log.warn("[Kafka Consumer] Thanh toán đơn hàng ID: {} thất bại. Cập nhật trạng thái FAILED.", order.getId());
      }
    } catch (Exception e) {
      log.error("[Kafka Consumer] Lỗi khi xử lý sự kiện payment-completed: ", e);
    }
  }
}
