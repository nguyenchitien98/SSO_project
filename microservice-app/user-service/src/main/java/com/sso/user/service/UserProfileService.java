package com.sso.user.service;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.user.entity.UserProfile;
import com.sso.user.repository.UserProfileRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp dịch vụ quản lý hồ sơ người dùng (User Profile Service).
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserProfileRepository userRepository;

  /**
   * Truy xuất hồ sơ người dùng, tự động tạo mới bản ghi rỗng nếu chưa tồn tại.
   *
   * @param userId UUID người dùng
   * @param email Email người dùng giải mã từ JWT
   * @return Đối tượng UserProfile
   */
  @Transactional
  public UserProfile getOrCreateProfile(UUID userId, String email) {
    return userRepository
        .findById(userId)
        .orElseGet(
            () -> {
              UserProfile newProfile =
                  UserProfile.builder()
                      .id(userId)
                      .displayName(email != null ? email.split("@")[0] : "User")
                      .avatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=" + userId)
                      .createdAt(Instant.now())
                      .updatedAt(Instant.now())
                      .build();
              return userRepository.save(newProfile);
            });
  }

  /**
   * Cập nhật hồ sơ thông tin người dùng.
   *
   * @param userId UUID người dùng hiện tại
   * @param updateReq Thông tin cập nhật đầu vào
   * @return Đối tượng UserProfile đã cập nhật
   */
  @Transactional
  public UserProfile updateProfile(UUID userId, UserProfile updateReq) {
    UserProfile profile =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người dùng"));

    profile.setDisplayName(updateReq.getDisplayName());
    profile.setPhone(updateReq.getPhone());
    profile.setBio(updateReq.getBio());
    if (updateReq.getAvatarUrl() != null) {
      profile.setAvatarUrl(updateReq.getAvatarUrl());
    }
    if (updateReq.getPreferences() != null) {
      profile.getPreferences().putAll(updateReq.getPreferences());
    }
    profile.setUpdatedAt(Instant.now());
    return userRepository.save(profile);
  }

  /**
   * Truy xuất hồ sơ người dùng theo ID (cho phép Admin/Support xem chéo).
   *
   * @param userId UUID người dùng cần tra cứu
   * @return Đối tượng UserProfile
   */
  @Transactional(readOnly = true)
  public UserProfile getProfileById(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(
            () ->
                new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người dùng"));
  }
}
