package com.sso.common.exception;

/**
 * Danh sách mã lỗi nghiệp vụ dùng chung cho toàn bộ hệ thống SSO Platform.
 *
 * <p>Mỗi mã lỗi ánh xạ tới một mã lỗi HTTP tương ứng để GlobalExceptionHandler
 * có thể trả về HTTP Status Code chính xác cho client.
 *
 * @author SSO Platform Team
 * @since Sprint 01
 */
public enum ErrorCode {
    /** Lỗi yêu cầu đăng nhập khi chưa được xác thực (HTTP 401). */
    UNAUTHORIZED(401, "Yêu cầu đăng nhập"),

    /** Lỗi Access Token hoặc Session đã hết hạn (HTTP 401). */
    TOKEN_EXPIRED(401, "Token đã hết hạn"),

    /** Lỗi Token không hợp lệ do chữ ký sai hoặc bị chỉnh sửa (HTTP 401). */
    INVALID_TOKEN(401, "Token không hợp lệ"),

    /** Lỗi không có quyền truy cập vào tài nguyên (HTTP 403). */
    FORBIDDEN(403, "Không có quyền thực hiện"),

    /** Lỗi không tìm thấy tài nguyên yêu cầu (HTTP 404). */
    NOT_FOUND(404, "Không tìm thấy tài nguyên"),

    /** Lỗi dữ liệu đầu vào của request không hợp lệ (HTTP 400). */
    INVALID_INPUT(400, "Dữ liệu đầu vào không hợp lệ"),

    /** Lỗi xung đột dữ liệu ví dụ trùng username, email (HTTP 409). */
    CONFLICT(409, "Xung đột dữ liệu"),

    /** Phát hiện tấn công brute force đăng nhập sai quá nhiều lần (HTTP 429). */
    BRUTE_FORCE_DETECTED(429, "Quá nhiều lần thử đăng nhập"),

    /** Vượt quá giới hạn tần suất gọi API cho phép (HTTP 429). */
    RATE_LIMIT_EXCEEDED(429, "Vượt tần suất gọi API"),

    /** Lỗi không mong muốn phát sinh từ phía server (HTTP 500). */
    INTERNAL_ERROR(500, "Lỗi hệ thống");

    private final int httpStatus;
    private final String defaultMessage;

    /**
     * Khởi tạo một ErrorCode với mã trạng thái HTTP và thông báo mặc định.
     *
     * @param httpStatus Mã HttpStatus Code tương ứng
     * @param defaultMessage Thông điệp mô tả lỗi mặc định
     */
    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Lấy mã trạng thái HTTP tương ứng của lỗi.
     *
     * @return Mã HttpStatus Code
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Lấy thông điệp mô tả lỗi mặc định bằng tiếng Việt.
     *
     * @return Thông điệp mặc định
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
