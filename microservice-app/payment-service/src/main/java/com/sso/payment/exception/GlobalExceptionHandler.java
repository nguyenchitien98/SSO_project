package com.sso.payment.exception;

import com.sso.common.dto.ApiResponse;
import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Tầng xử lý lỗi tập trung cho payment-service.
 *
 * @author SSO Platform Team
 * @since Sprint 12
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    log.warn(
        "Ngoại lệ nghiệp vụ phát sinh - ErrorCode: {}, Message: {}",
        errorCode,
        ex.getMessage());

    ApiResponse<Void> response = ApiResponse.error(errorCode, ex.getMessage());
    return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
    log.error("Ngoại lệ hệ thống không mong muốn phát sinh", ex);

    ApiResponse<Void> response =
        ApiResponse.error(ErrorCode.INTERNAL_ERROR, "Lỗi hệ thống không mong muốn phát sinh");
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus()).body(response);
  }
}
