package com.sso.monolith.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.monolith.dto.request.UpdateProfileRequest;
import com.sso.monolith.entity.UserProfile;
import com.sso.monolith.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cung cấp các endpoints xem và cập nhật hồ sơ cá nhân cục bộ trên Monolith.
 *
 * <p>Tại sao bảo mật endpoints bằng JWT? - Sử dụng `@AuthenticationPrincipal Jwt jwt` để đọc định
 * danh `sub` claim (SSO User UUID) một cách an toàn. - Cho phép phân quyền chéo bằng
 * `@PreAuthorize` (Ví dụ: kiểm tra quyền `USER_READ`, `USER_WRITE`).
 *
 * @author SSO Platform Team
 * @since Sprint 06
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

  private final UserProfileService userProfileService;

  /**
   * Lấy thông tin chi tiết hồ sơ cá nhân của người dùng hiện tại đang đăng nhập.
   *
   * <p>Yêu cầu quyền hạn: `USER_READ` (Được kiểm chứng động qua JWT claims mapping).
   */
  @GetMapping("/me")
  @PreAuthorize("hasAuthority('USER_READ')")
  public ResponseEntity<ApiResponse<UserProfile>> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    String name = jwt.getClaimAsString("name");
    log.info(
        "API GET /api/users/me - Yêu cầu truy vấn hồ sơ cho user chéo ID: {}, Name: {}",
        userId,
        name);

    UserProfile profile =
        userProfileService.getOrCreateProfile(userId, name != null ? name : "SSO User");
    return ResponseEntity.ok(ApiResponse.success(profile));
  }

  /**
   * Cập nhật thông tin hồ sơ cá nhân của người dùng hiện tại đang đăng nhập.
   *
   * <p>Yêu cầu quyền hạn: `USER_WRITE`.
   */
  @PutMapping("/me")
  @PreAuthorize("hasAuthority('USER_WRITE')")
  public ResponseEntity<ApiResponse<UserProfile>> updateMyProfile(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    log.info("API PUT /api/users/me - Yêu cầu cập nhật hồ sơ cho user chéo ID: {}", userId);

    UserProfile profile = userProfileService.updateProfile(userId, request);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ cá nhân thành công", profile));
  }
}
