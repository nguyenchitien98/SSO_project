package com.sso.payment.service;

import com.sso.payment.entity.OutboxEvent;
import com.sso.payment.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job quét bảng outbox_events và phát hành sự kiện thanh toán lên Kafka.
 *
 * @author SSO Platform Team
 * @since Sprint 16
 */
@Service
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class OutboxEventPublisher {

  private final OutboxEventRepository outboxRepository;
  private final KafkaTemplate<String, String> kafkaTemplate;

  /**
   * Quét và gửi các sự kiện PENDING lên Kafka mỗi 5 giây.
   */
  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void publishPendingEvents() {
    List<OutboxEvent> pendingEvents = outboxRepository.findAllByStatusOrderByCreatedAtAsc("PENDING");
    if (pendingEvents.isEmpty()) {
      return;
    }

    log.info("[Outbox Scheduler] Phát hiện {} sự kiện đang chờ gửi...", pendingEvents.size());

    for (OutboxEvent event : pendingEvents) {
      try {
        String topic = getTopicForEvent(event.getEventType());
        
        log.info("[Outbox Scheduler] Đang gửi sự kiện {} (ID: {}) tới topic: {}", 
            event.getEventType(), event.getId(), topic);

        // Gửi thông điệp lên Kafka
        kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get();

        // Cập nhật trạng thái thành công
        event.setStatus("SENT");
        event.setSentAt(Instant.now());
        outboxRepository.save(event);

        log.info("[Outbox Scheduler] Đã gửi thành công sự kiện ID: {}", event.getId());
      } catch (Exception e) {
        log.error("[Outbox Scheduler] Lỗi khi gửi sự kiện ID: {}. Sẽ thử lại sau. Chi tiết: {}", 
            event.getId(), e.getMessage());

        event.setRetryCount(event.getRetryCount() + 1);
        if (event.getRetryCount() >= 5) {
          event.setStatus("FAILED");
          log.error("[Outbox Scheduler] Sự kiện ID: {} đã đạt giới hạn thử lại (5 lần). Đánh dấu là FAILED.", event.getId());
        }
        outboxRepository.save(event);
      }
    }
  }

  private String getTopicForEvent(String eventType) {
    return switch (eventType) {
      case "PAYMENT_COMPLETED" -> "payment-completed";
      case "PAYMENT_FAILED" -> "payment-failed";
      default -> throw new IllegalArgumentException("Không xác định được topic cho loại sự kiện: " + eventType);
    };
  }
}
