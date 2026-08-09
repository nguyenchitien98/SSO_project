package com.sso.common.dto;

import com.sso.common.exception.ErrorCode;
import java.time.Instant;

/**
 * Wrapper chuẩn hóa cho tất cả các API responses trong hệ thống.
 *
 * <p>Đảm bảo Frontend luôn nhận cùng một cấu trúc dữ liệu JSON bất kể API thành công hay thất bại.
 * Cấu trúc này giúp đồng bộ hóa việc xử lý lỗi và dữ liệu trả về ở phía client.
 *
 * @param <T> Kiểu dữ liệu của payload trả về
 * @param success Trạng thái thành công của API (true/false)
 * @param message Thông điệp phản hồi từ API
 * @param data Dữ liệu kết quả thực tế
 * @param errorCode Mã lỗi dạng String (null nếu API thành công)
 * @param timestamp Thời điểm phản hồi được tạo ra
 * @author SSO Platform Team
 * @since Sprint 01
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String errorCode,
        Instant timestamp
) {
    /**
     * Tạo một ApiResponse thành công với thông điệp mặc định.
     *
     * @param data Dữ liệu trả về
     * @param <T> Kiểu dữ liệu của payload
     * @return ApiResponse biểu thị sự thành công
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Thành công", data, null, Instant.now());
    }

    /**
     * Tạo một ApiResponse thành công với thông điệp tự định nghĩa.
     *
     * @param message Thông điệp tùy chỉnh
     * @param data Dữ liệu trả về
     * @param <T> Kiểu dữ liệu của payload
     * @return ApiResponse biểu thị sự thành công
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    /**
     * Tạo một ApiResponse lỗi từ ErrorCode và thông điệp tùy chỉnh.
     *
     * @param code Đối tượng ErrorCode chứa mã lỗi nghiệp vụ
     * @param message Chi tiết lỗi cụ thể
     * @return ApiResponse biểu thị sự thất bại
     */
    public static ApiResponse<Void> error(ErrorCode code, String message) {
        return new ApiResponse<>(false, message, null, code.name(), Instant.now());
    }
}
