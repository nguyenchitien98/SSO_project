package com.sso.monolith.service;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.monolith.dto.request.UpdateProfileRequest;
import com.sso.monolith.entity.UserProfile;
import com.sso.monolith.repository.UserProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp dịch vụ (Service) xử lý các nghiệp vụ liên quan đến Hồ sơ người dùng (User Profile).
 *
 * <p>Tại sao cần lớp này? - Quản lý việc truy vấn, đồng bộ hóa tự động và cập nhật thông tin chi
 * tiết hồ sơ cục bộ. - Cho phép Monolith mở rộng các trường thông tin cá nhân (preferences, phone,
 * avatar) mà không cần sửa đổi bảng User dùng chung trên SSO Server.
 *
 * @author SSO Platform Team
 * @since Sprint 06
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserProfileService {

  private final UserProfileRepository userProfileRepository;

  /**
   * Truy vấn thông tin hồ sơ của người dùng. Nếu chưa tồn tại, tự động khởi tạo mặc định.
   *
   * <p>Tại sao tự động khởi tạo mặc định? - Để đảm bảo khi một người dùng đăng nhập SSO thành công
   * lần đầu và gọi API Monolith, hệ thống luôn trả về hồ sơ hợp lệ mà không phát sinh lỗi NotFound.
   *
   * @param id ID dạng UUID của người dùng từ SSO Server
   * @param defaultDisplayName Tên hiển thị mặc định lấy từ JWT claims (name)
   * @return Đối tượng UserProfile
   */
  @Transactional
  public UserProfile getOrCreateProfile(UUID id, String defaultDisplayName) {
    log.info("Lấy hoặc tạo hồ sơ người dùng chéo cho UUID: {}", id);
    return userProfileRepository
        .findById(id)
        .orElseGet(
            () -> {
              log.info(
                  "Không tìm thấy hồ sơ cục bộ. Tiến hành tạo mới hồ sơ mặc định cho: {}",
                  defaultDisplayName);
              UserProfile newProfile =
                  UserProfile.builder()
                      .id(id)
                      .displayName(defaultDisplayName)
                      .preferences("{}")
                      .build();
              return userProfileRepository.save(newProfile);
            });
  }

  /**
   * Cập nhật thông tin hồ sơ cá nhân cục bộ.
   *
   * @param id ID người dùng cần cập nhật
   * @param request DTO chứa dữ liệu mới
   * @return Thực thể UserProfile sau khi cập nhật
   */
  @Transactional
  public UserProfile updateProfile(UUID id, UpdateProfileRequest request) {
    log.info("Cập nhật thông tin hồ sơ cho người dùng UUID: {}", id);
    UserProfile profile =
        userProfileRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người dùng"));

    if (request.getDisplayName() != null) {
      profile.setDisplayName(request.getDisplayName());
    }
    if (request.getPhone() != null) {
      profile.setPhone(request.getPhone());
    }
    if (request.getAvatarUrl() != null) {
      profile.setAvatarUrl(request.getAvatarUrl());
    }
    if (request.getAddress() != null) {
      profile.setAddress(request.getAddress());
    }
    if (request.getPreferences() != null) {
      profile.setPreferences(request.getPreferences());
    }

    UserProfile savedProfile = userProfileRepository.save(profile);
    log.info("Cập nhật thành công hồ sơ cho người dùng: {}", savedProfile.getDisplayName());
    return savedProfile;
  }
}
