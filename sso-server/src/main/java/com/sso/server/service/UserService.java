package com.sso.server.service;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.server.dto.request.CreateUserRequest;
import com.sso.server.dto.request.UpdateUserRequest;
import com.sso.server.dto.response.UserResponse;
import com.sso.server.entity.Role;
import com.sso.server.entity.User;
import com.sso.server.repository.RoleRepository;
import com.sso.server.repository.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp dịch vụ (Service) xử lý các nghiệp vụ liên quan tới quản trị tài khoản Người dùng (User).
 *
 * <p>Tại sao cần lớp này? - Quản lý nghiệp vụ tạo tài khoản người dùng, mã hóa mật khẩu, cập nhật
 * thông tin và cập nhật trạng thái. - Tách biệt logic nghiệp vụ khỏi tầng Controller. - Hỗ trợ phân
 * trang danh sách người dùng để tối ưu hiệu năng.
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Tạo một người dùng mới trong hệ thống.
   *
   * <p>Tại sao băm mật khẩu trước khi lưu DB? - Để đảm bảo an toàn, phòng ngừa rò rỉ dữ liệu thô. -
   * BCrypt tạo salt tự động tăng độ khó chống brute-force.
   *
   * @param request DTO chứa thông tin tài khoản người dùng cần tạo
   * @return DTO phản hồi tài khoản người dùng đã tạo
   */
  @Transactional
  public UserResponse createUser(CreateUserRequest request) {
    log.info("Thực hiện tạo tài khoản người dùng mới: {}", request.getUsername());

    if (userRepository.findByUsername(request.getUsername()).isPresent()) {
      log.warn("Tạo người dùng thất bại: username '{}' đã tồn tại", request.getUsername());
      throw new BusinessException(
          ErrorCode.CONFLICT, "Tên đăng nhập '" + request.getUsername() + "' đã tồn tại");
    }

    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      log.warn("Tạo người dùng thất bại: email '{}' đã tồn tại", request.getEmail());
      throw new BusinessException(
          ErrorCode.CONFLICT, "Email '" + request.getEmail() + "' đã được sử dụng");
    }

    Set<Role> roles = new HashSet<>();
    for (String roleName : request.getRoles()) {
      Role role =
          roleRepository
              .findByName(roleName.toUpperCase())
              .orElseThrow(
                  () ->
                      new BusinessException(
                          ErrorCode.NOT_FOUND, "Không tìm thấy vai trò: " + roleName));
      roles.add(role);
    }

    User user =
        User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .roles(roles)
            .enabled(true)
            .locked(false)
            .build();

    User savedUser = userRepository.save(user);
    log.info("Tạo tài khoản thành công, ID: {}", savedUser.getId());
    return mapToUserResponse(savedUser);
  }

  /**
   * Lấy danh sách người dùng phân trang.
   *
   * @param pageable Tham số phân trang từ request
   * @return Trang thông tin người dùng DTO
   */
  public Page<UserResponse> getAllUsers(Pageable pageable) {
    log.info("Truy vấn danh sách tài khoản người dùng phân trang");
    return userRepository.findAll(pageable).map(this::mapToUserResponse);
  }

  /**
   * Tìm kiếm thông tin chi tiết của người dùng dựa trên ID.
   *
   * @param id ID của người dùng dạng UUID
   * @return DTO thông tin chi tiết người dùng
   */
  public UserResponse getUserById(UUID id) {
    log.info("Truy vấn chi tiết tài khoản ID: {}", id);
    User user =
        userRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy tài khoản với ID: " + id));
    return mapToUserResponse(user);
  }

  /**
   * Cập nhật thông tin cơ bản người dùng.
   *
   * @param id ID của người dùng cần cập nhật
   * @param request DTO chứa thông tin mới
   * @return DTO thông tin người dùng sau khi cập nhật
   */
  @Transactional
  public UserResponse updateUser(UUID id, UpdateUserRequest request) {
    log.info("Thực hiện cập nhật thông tin cho người dùng ID: {}", id);
    User user =
        userRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy tài khoản với ID: " + id));

    // Kiểm tra email trùng lặp với người khác
    userRepository
        .findByEmail(request.getEmail())
        .ifPresent(
            existingUser -> {
              if (!existingUser.getId().equals(id)) {
                log.warn(
                    "Cập nhật thất bại: email '{}' đã trùng với tài khoản khác",
                    request.getEmail());
                throw new BusinessException(
                    ErrorCode.CONFLICT, "Email '" + request.getEmail() + "' đã được sử dụng");
              }
            });

    user.setEmail(request.getEmail());
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setEnabled(request.isEnabled());
    user.setUpdatedAt(Instant.now());

    User updatedUser = userRepository.save(user);
    log.info("Cập nhật thông tin thành công cho tài khoản: {}", user.getUsername());
    return mapToUserResponse(updatedUser);
  }

  /**
   * Kích hoạt hoặc vô hiệu hóa một tài khoản người dùng.
   *
   * @param id ID người dùng
   * @param enabled Trạng thái mong muốn (true = kích hoạt, false = vô hiệu hóa)
   * @param reason Lý do thay đổi trạng thái
   * @return DTO thông tin người dùng sau khi cập nhật trạng thái
   */
  @Transactional
  public UserResponse updateUserStatus(UUID id, boolean enabled, String reason) {
    log.info("Thực hiện cập nhật trạng thái hoạt động ({}) cho người dùng ID: {}", enabled, id);
    User user =
        userRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy tài khoản với ID: " + id));

    user.setEnabled(enabled);
    if (!enabled) {
      user.setLocked(true);
      user.setLockedReason(reason);
    } else {
      user.setLocked(false);
      user.setLockedReason(null);
      user.setFailedLoginAttempts(0);
    }
    user.setUpdatedAt(Instant.now());

    User updatedUser = userRepository.save(user);
    log.info("Cập nhật trạng thái thành công cho tài khoản: {}", user.getUsername());
    return mapToUserResponse(updatedUser);
  }

  /**
   * Gán danh sách vai trò cho một người dùng.
   *
   * @param id ID của người dùng
   * @param roleNames Danh sách vai trò muốn gán
   * @return DTO thông tin người dùng
   */
  @Transactional
  public UserResponse assignRolesToUser(UUID id, Set<String> roleNames) {
    log.info("Thực hiện gán vai trò {} cho người dùng ID: {}", roleNames, id);
    User user =
        userRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy tài khoản với ID: " + id));

    Set<Role> roles = new HashSet<>();
    for (String roleName : roleNames) {
      Role role =
          roleRepository
              .findByName(roleName.toUpperCase())
              .orElseThrow(
                  () ->
                      new BusinessException(
                          ErrorCode.NOT_FOUND, "Không tìm thấy vai trò: " + roleName));
      roles.add(role);
    }

    user.setRoles(roles);
    user.setUpdatedAt(Instant.now());
    User updatedUser = userRepository.save(user);
    log.info("Gán vai trò thành công cho tài khoản: {}", user.getUsername());
    return mapToUserResponse(updatedUser);
  }

  /**
   * Thu hồi vai trò cụ thể khỏi người dùng.
   *
   * @param id ID người dùng
   * @param roleId ID vai trò cần thu hồi
   * @return DTO thông tin người dùng sau khi thu hồi
   */
  @Transactional
  public UserResponse removeRoleFromUser(UUID id, Long roleId) {
    log.info("Thực hiện thu hồi vai trò ID: {} khỏi người dùng ID: {}", roleId, id);
    User user =
        userRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy tài khoản với ID: " + id));

    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy vai trò với ID: " + roleId));

    if (!user.getRoles().contains(role)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "Người dùng không có vai trò này");
    }

    user.getRoles().remove(role);
    user.setUpdatedAt(Instant.now());
    User updatedUser = userRepository.save(user);
    log.info("Thu hồi vai trò thành công cho tài khoản: {}", user.getUsername());
    return mapToUserResponse(updatedUser);
  }

  /**
   * Hàm helper chuyển đổi thực thể {@link User} sang {@link UserResponse}.
   *
   * @param user Thực thể User cần chuyển đổi
   * @return DTO UserResponse
   */
  private UserResponse mapToUserResponse(User user) {
    Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

    return UserResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .enabled(user.isEnabled())
        .locked(user.isLocked())
        .lockedReason(user.getLockedReason())
        .lastLoginAt(user.getLastLoginAt())
        .roles(roles)
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
