package com.sso.file.service;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.file.entity.FileMetadata;
import com.sso.file.repository.FileMetadataRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Lớp dịch vụ quản lý lưu trữ tệp tin lên Object Storage MinIO (File Storage Service).
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

  private final S3Client s3Client;
  private final FileMetadataRepository metadataRepository;

  @Value("${minio.bucket-name}")
  private String bucketName;

  @Value("${minio.url}")
  private String minioUrl;

  /** Khởi tạo bucket nếu chưa tồn tại trên MinIO và cấu hình chính sách đọc công khai (Public Read). */
  @PostConstruct
  public void init() {
    try {
      try {
        s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        log.info("Bucket '{}' đã tồn tại sẵn trong MinIO.", bucketName);
      } catch (NoSuchBucketException e) {
        log.info("Bucket '{}' chưa tồn tại. Tiến hành khởi tạo mới.", bucketName);
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        setBucketPublicPolicy();
      }
    } catch (Exception e) {
      log.error("Lỗi khi kết nối hoặc khởi tạo bucket trong MinIO: {}", e.getMessage());
    }
  }

  /**
   * Tải tệp tin lên MinIO và lưu thông tin siêu dữ liệu vào cơ sở dữ liệu.
   *
   * @param file Tệp tin tải lên (MultipartFile)
   * @param userId UUID người dùng thực hiện
   * @return Siêu dữ liệu FileMetadata kèm đường dẫn URL tải xuống công khai
   */
  @Transactional
  public FileMetadata uploadFile(MultipartFile file, UUID userId) {
    if (file.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "Không thể tải lên tệp tin rỗng");
    }

    // Giới hạn kích thước tệp tin: 5MB
    long maxSizeBytes = 5 * 1024 * 1024;
    if (file.getSize() > maxSizeBytes) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "Kích thước tệp tin vượt quá hạn mức 5MB");
    }

    String fileName = file.getOriginalFilename();
    String mimeType = file.getContentType();
    
    // Sinh khóa lưu trữ độc nhất
    String fileExtension = "";
    if (fileName != null && fileName.contains(".")) {
      fileExtension = fileName.substring(fileName.lastIndexOf("."));
    }
    String objectKey = userId.toString() + "/" + UUID.randomUUID().toString() + fileExtension;

    try {
      log.info("Bắt đầu upload file {} lên MinIO bucket {}", fileName, bucketName);
      
      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(objectKey)
          .contentType(mimeType)
          .build();

      s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

      // Tạo đường dẫn URL tải xuống trực tiếp
      String fileUrl = String.format("%s/%s/%s", minioUrl, bucketName, objectKey);

      FileMetadata metadata = FileMetadata.builder()
          .userId(userId)
          .fileName(fileName)
          .storagePath(fileUrl)
          .mimeType(mimeType)
          .fileSize(file.getSize())
          .createdAt(Instant.now())
          .build();

      return metadataRepository.save(metadata);

    } catch (IOException e) {
      log.error("Lỗi đọc dữ liệu tệp tin khi upload", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Lỗi đọc dữ liệu tệp tin");
    } catch (Exception e) {
      log.error("Lỗi giao tiếp với MinIO server", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Lỗi lưu trữ tệp tin lên Object Storage");
    }
  }

  /** Thiết lập Bucket Policy cho phép đọc công khai (Public Read) để Frontend tải ảnh trực tiếp. */
  private void setBucketPublicPolicy() {
    String policyJson = String.format(
        "{\n" +
        "  \"Version\": \"2012-10-17\",\n" +
        "  \"Statement\": [\n" +
        "    {\n" +
        "      \"Effect\": \"Allow\",\n" +
        "      \"Principal\": \"*\",\n" +
        "      \"Action\": [\"s3:GetObject\"],\n" +
        "      \"Resource\": [\"arn:aws:s3:::%s/*\"]\n" +
        "    }\n" +
        "  ]\n" +
        "}",
        bucketName
    );

    try {
      s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
          .bucket(bucketName)
          .policy(policyJson)
          .build());
      log.info("Đã thiết lập chính sách truy cập công khai (Public Read) cho bucket '{}'.", bucketName);
    } catch (Exception e) {
      log.warn("Không thể cấu hình Bucket Policy tự động: {}", e.getMessage());
    }
  }
}
