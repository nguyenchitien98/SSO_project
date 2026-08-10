package com.sso.monolith.exception;

import com.sso.common.dto.ApiResponse;
import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Bộ xử lý ngoại lệ toàn cục (Global Exception Handler) cho Monolith App.
 *
 * <p>Bắt tất cả các loại exception xảy ra trong quá trình xử lý request tại các Controllers và đóng
 * gói chúng thành cấu trúc phản hồi {@link ApiResponse} JSON chuẩn hóa.
 *
 * @author SSO Platform Team
 * @since Sprint 06
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /** Bắt ngoại lệ nghiệp vụ {@link BusinessException} và map sang mã HTTP tương ứng. */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
    log.warn("Lỗi nghiệp vụ phát sinh: {} - {}", ex.getErrorCode(), ex.getMessage());
    ErrorCode errorCode = ex.getErrorCode();
    ApiResponse<Void> response = ApiResponse.error(errorCode, ex.getMessage());
    return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
  }

  /** Bắt lỗi validation dữ liệu đầu vào (@Valid / @Validated). */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException ex) {
    String detailMessage =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

    log.warn("Dữ liệu đầu vào không hợp lệ: {}", detailMessage);
    ApiResponse<Void> response = ApiResponse.error(ErrorCode.INVALID_INPUT, detailMessage);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /** Bắt lỗi phân quyền Spring Security AccessDeniedException và trả về HTTP 403 Forbidden. */
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
      org.springframework.security.access.AccessDeniedException ex) {
    log.warn("Lỗi phân quyền truy cập chéo: {}", ex.getMessage());
    ApiResponse<Void> response =
        ApiResponse.error(ErrorCode.FORBIDDEN, "Không có quyền thực hiện hành động này");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }

  /** Bắt các lỗi hệ thống không mong muốn còn lại. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
    log.error("Lỗi hệ thống không mong muốn phát sinh", ex);
    ApiResponse<Void> response =
        ApiResponse.error(ErrorCode.INTERNAL_ERROR, "Đã xảy ra lỗi hệ thống nghiêm trọng");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
