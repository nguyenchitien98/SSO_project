package com.sso.server.service;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.server.dto.request.CreateRoleRequest;
import com.sso.server.dto.response.RoleResponse;
import com.sso.server.entity.Permission;
import com.sso.server.entity.Role;
import com.sso.server.repository.PermissionRepository;
import com.sso.server.repository.RoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp dịch vụ (Service) xử lý các nghiệp vụ liên quan tới Vai trò (Role) và Quyền hạn (Permission).
 *
 * <p>Tại sao cần lớp này? - Quản lý tập trung logic tạo vai trò, gán quyền hạn. - Đảm bảo tính nhất
 * quán dữ liệu bằng @Transactional. - Đảm bảo phân tách trách nhiệm giữa Controller và JPA
 * Repository.
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoleService {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;

  /**
   * Tạo một vai trò mới trong hệ thống.
   *
   * <p>Tại sao kiểm tra tính duy nhất của tên vai trò ở mức Service? - Để trả về
   * BusinessException(ErrorCode.CONFLICT) rõ ràng thay vì lỗi cơ sở dữ liệu thô.
   *
   * @param request DTO chứa thông tin vai trò cần tạo
   * @return DTO phản hồi vai trò đã tạo
   */
  @Transactional
  public RoleResponse createRole(CreateRoleRequest request) {
    String roleName = request.getName().toUpperCase();
    log.info("Thực hiện tạo vai trò mới với tên: {}", roleName);

    if (roleRepository.findByName(roleName).isPresent()) {
      log.warn("Tạo vai trò thất bại do đã tồn tại vai trò tên: {}", roleName);
      throw new BusinessException(
          ErrorCode.CONFLICT, "Vai trò '" + roleName + "' đã tồn tại trong hệ thống");
    }

    Role role =
        Role.builder()
            .name(roleName)
            .description(request.getDescription())
            .permissions(new HashSet<>())
            .build();

    Role savedRole = roleRepository.save(role);
    log.info("Tạo vai trò thành công, ID: {}", savedRole.getId());
    return mapToRoleResponse(savedRole);
  }

  /**
   * Lấy toàn bộ danh sách vai trò hiện có trong hệ thống.
   *
   * @return Danh sách vai trò DTO
   */
  public List<RoleResponse> getAllRoles() {
    log.info("Truy vấn toàn bộ danh sách vai trò trong hệ thống");
    return roleRepository.findAll().stream()
        .map(this::mapToRoleResponse)
        .collect(Collectors.toList());
  }

  /**
   * Gán danh sách quyền hạn cho một vai trò cụ thể.
   *
   * <p>Tại sao ném exception khi không tìm thấy permission? - Để đảm bảo tính toàn vẹn hệ thống dữ
   * liệu phân quyền. Admin không thể gán quyền không tồn tại.
   *
   * @param roleId ID của vai trò
   * @param permissionNames Danh sách tên quyền cần gán (ví dụ: 'PRODUCT_READ', 'USER_CREATE')
   * @return DTO phản hồi vai trò sau khi đã cập nhật quyền hạn
   */
  @Transactional
  public RoleResponse assignPermissionsToRole(Long roleId, Set<String> permissionNames) {
    log.info("Thực hiện gán quyền hạn {} cho vai trò ID: {}", permissionNames, roleId);
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy vai trò với ID: " + roleId));

    Set<Permission> permissions = new HashSet<>();
    for (String permName : permissionNames) {
      Permission permission =
          permissionRepository
              .findByName(permName.toUpperCase())
              .orElseThrow(
                  () ->
                      new BusinessException(
                          ErrorCode.NOT_FOUND, "Không tìm thấy quyền hạn: " + permName));
      permissions.add(permission);
    }

    role.setPermissions(permissions);
    Role updatedRole = roleRepository.save(role);
    log.info("Cập nhật thành công quyền hạn cho vai trò: {}", role.getName());
    return mapToRoleResponse(updatedRole);
  }

  /**
   * Hàm helper chuyển đổi thực thể {@link Role} sang {@link RoleResponse}.
   *
   * @param role Thực thể Role cần chuyển đổi
   * @return DTO RoleResponse
   */
  private RoleResponse mapToRoleResponse(Role role) {
    Set<String> permissions =
        role.getPermissions().stream().map(Permission::getName).collect(Collectors.toSet());

    return RoleResponse.builder()
        .id(role.getId())
        .name(role.getName())
        .description(role.getDescription())
        .permissions(permissions)
        .build();
  }
}
