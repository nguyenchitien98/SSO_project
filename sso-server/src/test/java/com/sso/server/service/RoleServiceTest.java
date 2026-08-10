package com.sso.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.server.dto.request.CreateRoleRequest;
import com.sso.server.dto.response.RoleResponse;
import com.sso.server.entity.Permission;
import com.sso.server.entity.Role;
import com.sso.server.repository.PermissionRepository;
import com.sso.server.repository.RoleRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lớp kiểm thử đơn vị (Unit Test) cho {@link RoleService}.
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

  @Mock private RoleRepository roleRepository;

  @Mock private PermissionRepository permissionRepository;

  @InjectMocks private RoleService roleService;

  private CreateRoleRequest createRoleRequest;
  private Role role;
  private Permission permission;

  @BeforeEach
  void setUp() {
    createRoleRequest =
        CreateRoleRequest.builder().name("ADMIN").description("Toàn quyền hệ thống").build();

    permission =
        Permission.builder().id(1L).name("USER_READ").resource("USER").action("READ").build();

    role =
        Role.builder()
            .id(1L)
            .name("ADMIN")
            .description("Toàn quyền hệ thống")
            .permissions(new HashSet<>())
            .build();
  }

  /** Kiểm thử trường hợp tạo vai trò mới thành công (Happy Path). */
  @Test
  void createRole_Success() {
    when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
    when(roleRepository.save(any(Role.class))).thenReturn(role);

    RoleResponse response = roleService.createRole(createRoleRequest);

    assertNotNull(response);
    assertEquals(role.getName(), response.getName());
    verify(roleRepository, times(1)).save(any(Role.class));
  }

  /** Kiểm thử trường hợp tạo vai trò thất bại do trùng tên vai trò. */
  @Test
  void createRole_ConflictName() {
    when(roleRepository.findByName(anyString())).thenReturn(Optional.of(role));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> {
              roleService.createRole(createRoleRequest);
            });

    assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    assertTrue(exception.getMessage().contains("đã tồn tại"));
    verify(roleRepository, never()).save(any(Role.class));
  }

  /** Kiểm thử trường hợp gán quyền hạn cho vai trò thành công (Happy Path). */
  @Test
  void assignPermissionsToRole_Success() {
    when(roleRepository.findById(anyLong())).thenReturn(Optional.of(role));
    when(permissionRepository.findByName("USER_READ")).thenReturn(Optional.of(permission));
    when(roleRepository.save(any(Role.class))).thenReturn(role);

    RoleResponse response = roleService.assignPermissionsToRole(1L, Set.of("USER_READ"));

    assertNotNull(response);
    verify(roleRepository, times(1)).save(any(Role.class));
  }

  /** Kiểm thử gán quyền thất bại khi vai trò không tồn tại. */
  @Test
  void assignPermissionsToRole_RoleNotFound() {
    when(roleRepository.findById(anyLong())).thenReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> {
              roleService.assignPermissionsToRole(1L, Set.of("USER_READ"));
            });

    assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    assertTrue(exception.getMessage().contains("Không tìm thấy vai trò"));
  }

  /** Kiểm thử gán quyền thất bại khi quyền không tồn tại. */
  @Test
  void assignPermissionsToRole_PermissionNotFound() {
    when(roleRepository.findById(anyLong())).thenReturn(Optional.of(role));
    when(permissionRepository.findByName(anyString())).thenReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> {
              roleService.assignPermissionsToRole(1L, Set.of("INVALID_PERMISSION"));
            });

    assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    assertTrue(exception.getMessage().contains("Không tìm thấy quyền hạn"));
  }
}
