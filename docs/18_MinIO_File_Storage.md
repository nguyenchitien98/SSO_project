# SSO Platform - Centralized File Storage (MinIO)

Tài liệu này đặc tả kiến trúc lưu trữ tệp tin tập trung sử dụng **MinIO Object Storage** (tương thích API AWS S3) trong dự án **SSO Platform**.

---

## 1. Kiến Trúc Tổng Quan

Trong hệ thống, các microservice hoặc monolith không tự ý lưu trữ file trực tiếp lên ổ đĩa cứng của server chạy ứng dụng. Việc này giúp đảm bảo các service luôn **stateless**, dễ dàng scale ngang.

```
[Client/Frontend] ──> [API Gateway] ──> [file-service] ──> [MinIO Storage]
```

- **file-service (:8096):** Là microservice đóng vai trò entry point duy nhất để upload, download và sinh URL cho file.
- **MinIO (:9000 API / :9001 Console):** Object Storage lưu trữ dữ liệu tệp tin vật lý thực tế dưới dạng các Buckets.

---

## 2. Phân Chia Buckets & Cơ Chế Quyền (Bucket Policies)

Hệ thống phân chia thành 2 loại buckets chính dựa trên mức độ bảo mật:

### 2.1 Public Bucket (`sso-public-bucket`)
- **Mục đích:** Lưu trữ avatar người dùng, hình ảnh sản phẩm.
- **Quyền hạn (Policy):** **Read-Only công khai**. Bất kỳ ai cũng có thể đọc/truy cập trực tiếp file qua URL tĩnh (ví dụ: `http://localhost:9000/sso-public-bucket/avatar.jpg`).
- **Quy tắc ghi:** Chỉ duy nhất `file-service` được ghi (Write) thông qua AWS SDK credentials bí mật.

### 2.2 Private Bucket (`sso-private-bucket`)
- **Mục đích:** Lưu trữ các tài liệu nhạy cảm, hợp đồng đơn hàng, hóa đơn thanh toán.
- **Quyền hạn (Policy):** **Private hoàn toàn**. Mọi yêu cầu truy cập trực tiếp từ bên ngoài đều bị chặn (HTTP 403).
- **Quy tắc đọc/ghi:**
  - `file-service` đọc/ghi bằng AWS SDK.
  - Khi người dùng hợp lệ yêu cầu xem tài liệu, `file-service` sẽ dùng Private Key của mình để ký và sinh ra một **Presigned URL** có thời hạn ngắn (ví dụ: 15 phút). Người dùng sử dụng URL này để tải file trực tiếp từ MinIO trước khi nó hết hạn.

---

## 3. Cấu Trúc Code Java Tích Hợp (Spring Boot)

### 3.1 Maven Dependencies
Sử dụng AWS S3 SDK trong `file-service`:
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.0</version>
</dependency>
```

### 3.2 Cấu hình Client (S3Config.java)
```java
@Configuration
public class S3Config {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true) // Bắt buộc cho MinIO
                .build())
            .region(Region.US_EAST_1) // Region giả lập cho MinIO
            .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
            .region(Region.US_EAST_1)
            .build();
    }
}
```

### 3.3 File Storage Service (MinioStorageService.java)
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${minio.public-bucket}")
    private String publicBucket;

    @Value("${minio.private-bucket}")
    private String privateBucket;

    /**
     * Tải file lên public bucket.
     * Trả về URL trực tiếp truy cập file.
     */
    public String uploadPublicFile(String filename, byte[] content, String contentType) {
        String uniqueFilename = UUID.randomUUID().toString() + "_" + filename;
        
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(publicBucket)
            .key(uniqueFilename)
            .contentType(contentType)
            .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(content));
        
        // Trả về public url để client truy cập trực tiếp
        return String.format("%s/%s/%s", "http://localhost:9000", publicBucket, uniqueFilename);
    }

    /**
     * Sinh Presigned URL cho phép truy cập tệp tin private trong thời gian giới hạn.
     */
    public String generatePresignedUrl(String fileKey, Duration duration) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(privateBucket)
            .key(fileKey)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(duration)
            .getObjectRequest(getRequest)
            .build();

        PresignedGetObjectRequest presignedResult = s3Presigner.presignGetObject(presignRequest);
        return presignedResult.url().toString();
    }
}
```

---

## 4. Các Quy Tắc Bảo Mật File Upload Bắt Buộc

1. **Kiểm tra File Size:** Giới hạn dung lượng tải lên (tối đa 5MB cho ảnh avatar, 20MB cho các tài liệu khác) để tránh tấn công từ chối dịch vụ (DoS) làm đầy dung lượng ổ cứng MinIO.
2. **Kiểm tra MIME Type thực tế:** Không tin cậy vào đuôi mở rộng của tệp tin do client gửi lên (ví dụ: `.jpg`, `.png`). Sử dụng thư viện phân tích nội dung thực tế (như Apache Tika) để phát hiện nếu tệp tin là mã độc thực thi nguy hiểm.
3. **Sinh tên file ngẫu nhiên:** Sử dụng `UUID` sinh ngẫu nhiên làm tên file trên MinIO để tránh trùng lặp tên file và ngăn chặn tấn công chèn ký tự đặc biệt thay đổi đường dẫn (Path Traversal).
