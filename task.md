# Tasks - Sprint 16: Kafka Event-Driven & Outbox Pattern

- `[x]` Khai báo cấu hình kết nối Apache Kafka chung trong config-server
- `[x]` Thiết lập 4 Kafka topics tự động khởi tạo bằng Spring Bean trong `KafkaTopicConfig.java` tại `order-service`
- `[x]` Triển khai Transactional Outbox Pattern cho `order-service` (lưu thực thể `OutboxEvent` cùng transaction khi tạo đơn)
- `[x]` Xây dựng Scheduled Job `OutboxEventPublisher.java` trong `order-service` quét và phát hành sự kiện lên Kafka
- `[x]` Triển khai Outbox Pattern tương tự cho `payment-service` (khi giao dịch thanh toán chuyển thành `COMPLETED` sau 2 giây)
- `[x]` Triển khai consumer `PaymentCompletedListener.java` trong `order-service` cập nhật đơn hàng thành `PAID`
- `[x]` Triển khai consumer `OrderCreatedListener.java` trong `notification-service` giả lập gửi email
- `[x]` Tích hợp kiểm duyệt trùng lặp (Idempotent Consumer) cho `OrderCreatedListener` bằng cách lưu UUID sự kiện vào Redis
- `[x]` Phát triển endpoint API `GET /api/orders/reports` tại `OrderController` tính toán doanh thu thực tế từ database
- `[x]` Nâng cấp Next.js BFF proxy `/api/orders/reports` chuyển tiếp token an toàn
- `[x]` Cập nhật UI dashboard báo cáo `ReportsPage` fetch dữ liệu real-time từ API Gateway vẽ biểu đồ Chart.js
- `[x]` Thực thi đóng gói Maven và chạy toàn bộ kiểm thử thành công
