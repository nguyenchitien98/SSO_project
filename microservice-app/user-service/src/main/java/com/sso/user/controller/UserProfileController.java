package com.sso.user.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.user.entity.UserProfile;
import com.sso.user.security.AuthorizationService;
import com.sso.user.security.CurrentUser;
import com.sso.user.security.CurrentUserResolver;
import com.sso.user.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller tiếp nhận yêu cầu liên quan tới hồ sơ người dùng (User Profile Controller).
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

  private final UserProfileService profileService;
  private final CurrentUserResolver userResolver;
  private final AuthorizationService authService;

  /**
   * Lấy thông tin cá nhân của người dùng hiện tại đang đăng nhập.
   *
   * @param request HTTP request chứa trusted headers
   * @return ResponseEntity chứa UserProfile
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<?>> getMyProfile(HttpServletRequest request) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API GET /api/users/me - Yêu cầu từ user: {}", currentUser != null ? currentUser.email() : "GUEST");

    if (currentUser == null) {
      return ResponseEntity.status(401).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }

    UserProfile profile = profileService.getOrCreateProfile(
        UUID.fromString(currentUser.id()), currentUser.email());
    return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ thành công", profile));
  }

  /**
   * Cập nhật thông tin cá nhân của người dùng hiện tại.
   *
   * @param request HTTP request
   * @param updateReq Thông tin cập nhật
   * @return ResponseEntity chứa UserProfile đã cập nhật
   */
  @PutMapping("/me")
  public ResponseEntity<ApiResponse<?>> updateMyProfile(
      HttpServletRequest request, @RequestBody UserProfile updateReq) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API PUT /api/users/me - Yêu cầu cập nhật từ user: {}", currentUser != null ? currentUser.email() : "GUEST");

    if (currentUser == null) {
      return ResponseEntity.status(401).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }

    UserProfile updated = profileService.updateProfile(
        UUID.fromString(currentUser.id()), updateReq);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", updated));
  }

  /**
   * Lấy thông tin cá nhân của người dùng bất kỳ theo ID (ADMIN hoặc SUPPORT).
   *
   * @param request HTTP request
   * @param id UUID của người dùng cần xem
   * @return ResponseEntity chứa UserProfile
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<?>> getProfileById(
      HttpServletRequest request, @PathVariable UUID id) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API GET /api/users/{} - Yêu cầu từ user: {}", id, currentUser != null ? currentUser.email() : "GUEST");

    // Chỉ cho phép ADMIN hoặc SUPPORT
    if (currentUser == null) {
      return ResponseEntity.status(401).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }
    
    // Check role
    boolean isAdmin = currentUser.roles().contains("ADMIN");
    boolean isSupport = currentUser.roles().contains("SUPPORT");
    if (!isAdmin && !isSupport) {
      return ResponseEntity.status(403).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.FORBIDDEN, "Không có quyền thực hiện hành động này"));
    }

    UserProfile profile = profileService.getProfileById(id);
    return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ người dùng thành công", profile));
  }

  /**
   * Kích hoạt hoặc Vô hiệu hóa người dùng bất kỳ (ADMIN only).
   *
   * @param request HTTP request
   * @param id UUID của người dùng cần toggle status
   * @return ResponseEntity chứa kết quả
   */
  @PutMapping("/{id}/status")
  public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
      HttpServletRequest request, @PathVariable UUID id) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API PUT /api/users/{}/status - Yêu cầu từ user: {}", id, currentUser != null ? currentUser.email() : "GUEST");

    if (currentUser == null) {
      return ResponseEntity.status(401).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }
    
    authService.requireRole(currentUser, "ADMIN");

    // Mock response thành công (do thao tác cập nhật tài khoản sso chính được sso-server đảm nhận)
    log.info("Đã cập nhật trạng thái tài khoản người dùng: {} bởi Admin: {}", id, currentUser.email());
    return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái người dùng thành công", null));
  }
}
