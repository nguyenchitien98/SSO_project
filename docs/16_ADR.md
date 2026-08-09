# SSO Platform - Nhật Ký Quyết Định Kiến Trúc (Architecture Decision Records - ADR)

Tài liệu này ghi lại các quyết định thiết kế kiến trúc quan trọng trong hệ thống **SSO Platform**, giải thích bối cảnh, các giải pháp thay thế đã được cân nhắc và lý do chọn phương án tối ưu.

---

## ADR-001: Tại sao dùng Spring Authorization Server thay vì Keycloak cho dự án code?

### Bối cảnh (Context)
Hệ thống cần một Identity Provider (IdP) hỗ trợ OAuth2/OIDC chuẩn để xác thực người dùng. Hai phương án phổ biến là sử dụng một sản phẩm đóng gói sẵn như **Keycloak** hoặc tự viết code dựa trên **Spring Authorization Server**.

### Các phương án cân nhắc (Options)
1. **Keycloak:** Chạy độc lập qua Docker, có sẵn giao diện quản trị Admin Console, đầy đủ tính năng 2FA, Social Login.
2. **Spring Authorization Server (SAS):** Framework thư viện chính thức từ Spring, lập trình viên tự cấu hình và viết code Java để xử lý token, login flow, clients.

### Quyết định (Decision)
**Lựa chọn:** Spring Authorization Server.
*Lưu ý:* Sơ đồ kiến trúc `ArchitectureSSO.png` có vẽ Keycloak để thể hiện thiết kế chuẩn doanh nghiệp có thể thay thế. Nhưng trong source code dự án, ta chọn SAS.

### Lý do chọn (Consequences / Rationale)
- **Giá trị học tập (Learning Curve):** Giúp lập trình viên hiểu sâu từng bước của OAuth2 Authorization Server (ví dụ: cách custom claims vào JWT, tự thiết kế cấu trúc database lưu trữ client, session). Keycloak che giấu hoàn toàn các chi tiết này đằng sau giao diện cấu hình.
- **Tùy biến cao bằng Code:** Dễ dàng tích hợp các tính năng custom của dự án như thuật toán mã hóa 2FA riêng, ghi audit log nghiệp vụ trực tiếp vào DB của Auth Server, và link tài khoản Social theo logic nghiệp vụ mong muốn mà không cần viết các SPI (Service Provider Interface) phức tạp bằng Java cho Keycloak.
- **Tiết kiệm tài nguyên:** SAS chạy trực tiếp trong một Spring Boot application gọn nhẹ (~150-200MB RAM), Keycloak yêu cầu tài nguyên lớn hơn đáng kể (>1GB RAM) khi khởi chạy ở môi trường local.

---

## ADR-002: Tại sao Gateway validate JWT thay vì gọi Auth Service ở mỗi request?

### Bối cảnh
Trong hệ thống microservice, mỗi khi có request gửi vào, hệ thống phải xác định xem token đó có hợp lệ hay không trước khi forward tới các service bên trong.

### Các phương án cân nhắc
1. **Giao tiếp RPC (Stateful check):** Gateway gọi API của SSO Server để check tính hợp lệ của token cho mọi request.
2. **Xác thực Offline (Stateless validation):** Gateway tự giải mã và kiểm tra signature của JWT bằng public key lấy từ JWKS.

### Quyết định
**Lựa chọn:** Xác thực Offline tại API Gateway bằng JWKS.

### Lý do chọn
- **Hiệu năng & Latency:** Gateway tự validate JWT bằng tính toán mật mã nội bộ (CPU-bound) cực kỳ nhanh (< 1ms). Nếu gọi RPC đến SSO Server, sẽ thêm một network hop (tăng latency 10ms - 50ms) và gây nghẽn (bottleneck) tại SSO Server.
- **Decoupling (Độc lập):** Nếu SSO Server tạm thời bị down, API Gateway vẫn có thể tiếp tục validate các JWT còn hạn sử dụng từ Client và chuyển tiếp request đến microservices, giúp tăng tính sẵn sàng của hệ thống.

---

## ADR-003: Tại sao dùng Asymmetric Key (RSA) thay vì Shared Secret (HS256) cho JWT?

### Bối cảnh
Cần chọn thuật toán ký số (Signature) cho JWT.

### Các phương án cân nhắc
1. **Symmetric (Symmetric Key - HS256):** SSO Server và tất cả service sử dụng chung một mã bí mật (shared secret) để ký và verify JWT.
2. **Asymmetric (Asymmetric Key - RS256):** SSO Server giữ Private Key để ký. Gateway và các services chỉ giữ Public Key để xác thực.

### Quyết định
**Lựa chọn:** Asymmetric Key (RS256).

### Lý do chọn
- **Phân chia trách nhiệm an toàn:** Chỉ SSO Server mới có quyền phát hành và ký JWT mới (vì chỉ nó giữ Private Key). Các service khác dù bị hacker tấn công chiếm quyền cũng không thể giả mạo chữ ký để tạo ra JWT giả.
- **Key Rotation (Quay vòng khóa):** Dễ dàng triển khai quay vòng khóa thông qua JWKS endpoint (`/oauth2/jwks`). SSO Server sinh cặp khóa mới và công khai public key mới, các client tự động kéo về mà không cần khởi động lại hay cấu hình lại thủ công shared secret.

---

## ADR-004: @PreAuthorize ở Monolith vs AuthorizationService ở Microservice

### Bối cảnh
Cơ chế kiểm soát phân quyền (Authorization) ở Monolith và Microservice nên được thiết kế như thế nào?

### Các phương án cân nhắc
1. **Dùng chung Spring Security `@PreAuthorize` cho cả hai:** Tích hợp Spring Security vào tất cả microservices.
2. **Tách biệt cơ chế:** Dùng `@PreAuthorize` ở Monolith, dùng `AuthorizationService` thủ công ở Microservices.

### Quyết định
**Lựa chọn:** Tách biệt cơ chế (Phương án 2).

### Lý do chọn
- **Monolith:** Có sẵn Security Context đầy đủ nhờ tích hợp Spring Security filter chain trực tiếp. Sử dụng `@PreAuthorize` và SpEL expression giúp lập trình viên viết code rất nhanh và an toàn ngay tại Service layer.
- **Microservices:** Các service nội bộ chạy sau Gateway (Trust Boundary) đã nhận được các header tường minh như `X-User-Id`, `X-User-Permissions`. Việc lôi cả thư viện Spring Security cồng kềnh vào từng microservice nhỏ là không cần thiết, làm chậm thời gian startup và tăng dung lượng RAM. Thay vào đó, một `AuthorizationService` viết tay đơn giản đọc thông tin từ `CurrentUser` object sẽ trực quan, dễ viết Unit test và dễ debug hơn nhiều.

---

## ADR-005: Tại sao dùng Outbox Pattern thay vì publish Kafka trực tiếp trong Transaction?

### Bối cảnh
Khi Order Service lưu đơn hàng vào DB thành công, nó cần thông báo cho Notification Service gửi mail thông qua Kafka.

### Các phương án cân nhắc
1. **Publish trực tiếp:** Viết `kafkaTemplate.send()` ngay trong method `@Transactional` lưu Order.
2. **Transactional Outbox Pattern:** Lưu event vào bảng `outbox_events` cùng transaction với Order, sau đó có một tiến trình riêng (publisher) đọc ra gửi vào Kafka.

### Quyết định
**Lựa chọn:** Transactional Outbox Pattern.

### Lý do chọn
- **Đảm bảo tính nhất quán dữ liệu (Dual-write problem):** Nếu gửi trực tiếp, có trường hợp DB commit thành công nhưng mạng bị ngắt làm mất kết nối tới Kafka, dẫn tới Order đã tạo nhưng email không bao giờ được gửi. Hoặc ngược lại, gửi Kafka thành công nhưng DB bị rollback do lỗi ở bước sau, dẫn tới gửi email cho đơn hàng không tồn tại.
- **Đảm bảo At-least-once delivery:** Sự kiện luôn được lưu lại ở DB trước. Nếu Kafka broker bị sập, tiến trình publisher sẽ thử lại (retry) liên tục cho đến khi gửi thành công, đảm bảo không bao giờ bị mất event.

---

## ADR-006: Tại sao dùng Refresh Token Rotation và cách detect replay attack?

### Bối cảnh
Refresh Token có thời hạn dài (7 ngày). Nếu bị kẻ tấn công đánh cắp, chúng có thể âm thầm dùng nó để lấy Access Token mới liên tục.

### Quyết định
**Lựa chọn:** Bật **Refresh Token Rotation (RTR)** kèm cơ chế phát hiện sử dụng lại (**Replay Detection**).

### Lý do chọn
- **Tự động vô hiệu hóa:** Mỗi lần client gửi Refresh Token cũ để lấy Access Token mới, SSO Server sẽ trả về một Refresh Token mới và đánh dấu Refresh Token cũ là hết hiệu lực (`revoked = true`).
- **Phát hiện tấn công ngay lập tức:** Nếu kẻ tấn công (hoặc client bị lỗi) gửi lại một Refresh Token đã bị đánh dấu là `revoked`, SSO Server ngay lập tức phát hiện có hành vi gian lận (Replay Attack). Để đảm bảo an toàn tối đa, hệ thống sẽ thu hồi toàn bộ các Refresh Token thuộc cùng một dòng họ (`family_id`), buộc người dùng thật phải đăng nhập lại từ đầu và ghi nhận cảnh báo bảo mật.

---

## ADR-007: Tại sao dùng MinIO làm Object Storage?

### Bối cảnh
Hệ thống cần lưu trữ các file tĩnh như ảnh avatar người dùng, hình ảnh sản phẩm.

### Các phương án cân nhắc
1. **Lưu trữ trực tiếp trên Local Disk (File system):** Lưu file vào ổ cứng của VM chạy service.
2. **Cloud Object Storage (AWS S3, Google Cloud Storage):** Đẩy file lên cloud.
3. **MinIO Object Storage:** Chạy một container MinIO ở local làm Object Storage.

### Quyết định
**Lựa chọn:** MinIO Object Storage.

### Lý do chọn
- **Tương thích hoàn toàn API S3:** MinIO sử dụng chung tập lệnh API với AWS S3. Nhờ đó, code tích hợp Spring Boot sử dụng AWS SDK có thể chuyển đổi dễ dàng từ môi trường local (MinIO) sang môi trường Cloud Production (AWS S3) chỉ bằng việc thay đổi cấu hình endpoint url.
- **Không phụ thuộc Cloud (Cloud Agnostic):** Phù hợp cho môi trường phát triển local độc lập không cần internet và tài khoản thẻ tín dụng cloud.
- **Bảo mật tối đa:** Hỗ trợ đầy đủ các tính năng bảo mật của S3 như Bucket Policy, Presigned URLs, Access Control List (ACL).
