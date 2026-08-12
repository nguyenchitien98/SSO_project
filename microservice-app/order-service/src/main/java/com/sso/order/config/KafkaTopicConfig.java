package com.sso.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Cấu hình tự động tạo các topics Kafka lúc khởi chạy.
 *
 * @author SSO Platform Team
 * @since Sprint 16
 */
@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic orderCreatedTopic() {
    return TopicBuilder.name("order-created")
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic paymentCompletedTopic() {
    return TopicBuilder.name("payment-completed")
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic paymentFailedTopic() {
    return TopicBuilder.name("payment-failed")
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic orderStatusChangedTopic() {
    return TopicBuilder.name("order-status-changed")
        .partitions(3)
        .replicas(1)
        .build();
  }
}
