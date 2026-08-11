package com.sso.file.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.file.entity.FileMetadata;
import com.sso.file.security.CurrentUser;
import com.sso.file.security.CurrentUserResolver;
import com.sso.file.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller tiếp nhận yêu cầu tải lên tệp tin (File Controller).
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

  private final FileStorageService fileStorageService;
  private final CurrentUserResolver userResolver;

  /**
   * Tải tệp tin lên hệ thống lưu trữ (MinIO Object Storage).
   *
   * @param request HTTP Request
   * @param file Tệp tin gửi lên từ form (MultipartFile)
   * @return ResponseEntity chứa thông tin siêu dữ liệu tệp tin đã tải lên
   */
  @PostMapping("/upload")
  public ResponseEntity<ApiResponse<?>> uploadFile(
      HttpServletRequest request, @RequestParam("file") MultipartFile file) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API POST /api/files/upload - Yêu cầu từ user: {}", currentUser != null ? currentUser.email() : "GUEST");

    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }

    FileMetadata metadata = fileStorageService.uploadFile(file, UUID.fromString(currentUser.id()));
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Upload tệp tin thành công", metadata));
  }
}
