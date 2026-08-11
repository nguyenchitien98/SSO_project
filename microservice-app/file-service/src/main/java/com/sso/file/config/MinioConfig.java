package com.sso.file.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Cấu hình khởi tạo S3Client kết nối tới Object Storage MinIO.
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@Configuration
public class MinioConfig {

  @Value("${minio.url}")
  private String minioUrl;

  @Value("${minio.access-key}")
  private String accessKey;

  @Value("${minio.secret-key}")
  private String secretKey;

  /**
   * Khai báo bean S3Client phục vụ upload/download file từ MinIO.
   *
   * @return S3Client instance
   */
  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(minioUrl))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
        .region(Region.US_EAST_1) // MinIO sử dụng region mặc định US_EAST_1
        .serviceConfiguration(
            S3Configuration.builder()
                .pathStyleAccessEnabled(true) // Bắt buộc bật true đối với MinIO local
                .build())
        .build();
  }
}
