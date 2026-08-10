package com.sso.common.exception;

/**
 * Exception nghiệp vụ dùng chung cho tất cả các modules trong SSO Platform.
 *
 * <p>Exception này đóng gói một {@link ErrorCode} để các tầng xử lý lỗi (GlobalExceptionHandler) có
 * thể tự động ánh xạ lỗi nghiệp vụ ra mã phản hồi HTTP và cấu trúc JSON đồng bộ.
 *
 * @author SSO Platform Team
 * @since Sprint 01
 */
public class BusinessException extends RuntimeException {
  private final ErrorCode errorCode;

  /**
   * Khởi tạo ngoại lệ nghiệp vụ với mã lỗi và thông báo chi tiết.
   *
   * @param errorCode Đối tượng ErrorCode tương ứng
   * @param message Mô tả chi tiết nguyên nhân lỗi nghiệp vụ
   */
  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * Khởi tạo ngoại lệ nghiệp vụ với mã lỗi và thông báo mặc định của ErrorCode.
   *
   * @param errorCode Đối tượng ErrorCode tương ứng
   */
  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
  }

  /**
   * Lấy ErrorCode đi kèm ngoại lệ này.
   *
   * @return Đối tượng ErrorCode
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
