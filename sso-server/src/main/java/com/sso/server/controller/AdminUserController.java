package com.sso.server.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.server.dto.request.CreateUserRequest;
import com.sso.server.dto.request.UpdateUserRequest;
import com.sso.server.dto.response.UserResponse;
import com.sso.server.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các API quản lý tài khoản người dùng (Users) của hệ thống.
 *
 * <p>Tất cả các API trong Controller này bắt đầu với đường dẫn `/admin` và được bảo vệ bởi cấu hình
 * bảo mật ở tầng Gateway/Resource Server (yêu cầu token hợp lệ của admin client).
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

  private final UserService userService;

  /**
   * Tạo mới một tài khoản người dùng trong hệ thống.
   *
   * @param request DTO chứa thông tin tài khoản cần tạo
   * @return ResponseEntity chứa DTO phản hồi tài khoản đã tạo
   */
  @PostMapping
  public ResponseEntity<ApiResponse<UserResponse>> createUser(
      @Valid @RequestBody CreateUserRequest request) {
    log.info("API POST /admin/users - Bắt đầu tạo người dùng: {}", request.getUsername());
    UserResponse response = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo tài khoản người dùng thành công", response));
  }

  /**
   * Lấy danh sách tài khoản người dùng phân trang.
   *
   * @param pageable Tham số phân trang từ request
   * @return ResponseEntity chứa trang thông tin người dùng DTO
   */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(Pageable pageable) {
    log.info("API GET /admin/users - Truy vấn danh sách người dùng phân trang");
    Page<UserResponse> response = userService.getAllUsers(pageable);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Truy vấn thông tin chi tiết của một người dùng theo ID.
   *
   * @param id ID dạng UUID của người dùng cần xem
   * @return ResponseEntity chứa thông tin chi tiết người dùng DTO
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
    log.info("API GET /admin/users/{} - Truy vấn chi tiết người dùng", id);
    UserResponse response = userService.getUserById(id);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Cập nhật thông tin hồ sơ của tài khoản người dùng.
   *
   * @param id ID của người dùng cần cập nhật
   * @param request DTO chứa thông tin mới
   * @return ResponseEntity chứa thông tin người dùng DTO sau cập nhật
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> updateUser(
      @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
    log.info("API PUT /admin/users/{} - Cập nhật hồ sơ tài khoản", id);
    UserResponse response = userService.updateUser(id, request);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", response));
  }

  /**
   * Kích hoạt hoặc vô hiệu hóa một tài khoản người dùng.
   *
   * @param id ID của người dùng cần thay đổi trạng thái
   * @param statusPayload Body chứa 'enabled' (boolean) và 'reason' (string)
   * @return ResponseEntity chứa thông tin người dùng DTO sau thay đổi trạng thái
   */
  @PutMapping("/{id}/status")
  public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
      @PathVariable UUID id, @RequestBody Map<String, Object> statusPayload) {
    boolean enabled = (Boolean) statusPayload.getOrDefault("enabled", true);
    String reason = (String) statusPayload.getOrDefault("reason", "");

    log.info(
        "API PUT /admin/users/{}/status - Cập nhật hoạt động: {}, Lý do: {}", id, enabled, reason);
    UserResponse response = userService.updateUserStatus(id, enabled, reason);
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật trạng thái hoạt động thành công", response));
  }

  /**
   * Gán danh sách vai trò cho tài khoản người dùng.
   *
   * @param id ID của người dùng cần gán vai trò
   * @param roleNames Danh sách vai trò mới (Role Names)
   * @return ResponseEntity chứa thông tin người dùng DTO sau gán
   */
  @PostMapping("/{id}/roles")
  public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
      @PathVariable UUID id, @RequestBody Set<String> roleNames) {
    log.info("API POST /admin/users/{}/roles - Gán các vai trò: {}", id, roleNames);
    UserResponse response = userService.assignRolesToUser(id, roleNames);
    return ResponseEntity.ok(ApiResponse.success("Gán vai trò thành công", response));
  }

  /**
   * Thu hồi một vai trò cụ thể khỏi người dùng.
   *
   * @param id ID của người dùng
   * @param roleId ID của vai trò muốn thu hồi
   * @return ResponseEntity không chứa nội dung
   */
  @DeleteMapping("/{id}/roles/{roleId}")
  public ResponseEntity<ApiResponse<UserResponse>> removeRole(
      @PathVariable UUID id, @PathVariable Long roleId) {
    log.info("API DELETE /admin/users/{}/roles/{} - Thu hồi vai trò", id, roleId);
    UserResponse response = userService.removeRoleFromUser(id, roleId);
    return ResponseEntity.ok(ApiResponse.success("Thu hồi vai trò thành công", response));
  }
}
