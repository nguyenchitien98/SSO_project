package com.sso.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sso.common.event.OrderCreatedEvent;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe sự kiện tạo đơn hàng từ Kafka để thực hiện tác vụ gửi thông báo/email.
 *
 * @author SSO Platform Team
 * @since Sprint 16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedListener {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  private static final String PROCESSED_EVENT_PREFIX = "processed_event:order_created:";

  /**
   * Lắng nghe và xử lý sự kiện order-created.
   *
   * @param message Nội dung thông điệp dạng JSON String
   */
  @KafkaListener(topics = "order-created", groupId = "notification-group")
  public void consumeOrderCreated(String message) {
    try {
      log.info("[Kafka Consumer] Nhận được sự kiện order-created: {}", message);
      OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

      String redisKey = PROCESSED_EVENT_PREFIX + event.eventId();

      // Kiểm tra trùng lặp thông điệp (Idempotent Consumer) bằng Redis SETNX (TTL 7 ngày)
      Boolean isAbsent = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSED", 7, TimeUnit.DAYS);
      
      if (Boolean.FALSE.equals(isAbsent)) {
        log.warn("[Idempotent Consumer] Sự kiện ID: {} đã được xử lý trước đó. Bỏ qua.", event.eventId());
        return;
      }

      log.info("[Idempotent Consumer] Sự kiện ID: {} là hợp lệ. Bắt đầu gửi email xác nhận...", event.eventId());

      // Mock tác vụ gửi email
      sendMockEmail(event);

    } catch (Exception e) {
      log.error("[Kafka Consumer] Lỗi khi xử lý sự kiện order-created: ", e);
    }
  }

  private void sendMockEmail(OrderCreatedEvent event) {
    log.info("----------------------------------------------------------------------");
    log.info("📧 [MOCK EMAIL SERVICE] Gửi email xác nhận thành công!");
    log.info("   - Gửi tới: User ID {}", event.userId());
    log.info("   - Mã đơn hàng: {}", event.orderCode());
    log.info("   - Tổng thanh toán: {} VND", event.totalAmount());
    log.info("   - Chi tiết: {} sản phẩm", event.items().size());
    log.info("----------------------------------------------------------------------");
  }
}
