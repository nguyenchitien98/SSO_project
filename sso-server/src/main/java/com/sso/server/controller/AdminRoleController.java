package com.sso.server.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.server.dto.request.CreateRoleRequest;
import com.sso.server.dto.response.RoleResponse;
import com.sso.server.service.RoleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các API quản lý vai trò và quyền hạn (Roles & Permissions) của hệ thống.
 *
 * <p>Tất cả các API trong Controller này bắt đầu với đường dẫn `/admin` và được bảo vệ bởi cấu hình
 * bảo mật ở tầng Gateway/Resource Server (yêu cầu token hợp lệ của admin client).
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
@Slf4j
public class AdminRoleController {

  private final RoleService roleService;

  /**
   * Tạo mới một vai trò trong hệ thống.
   *
   * @param request DTO chứa thông tin vai trò cần tạo
   * @return ResponseEntity chứa DTO phản hồi vai trò đã tạo
   */
  @PostMapping
  public ResponseEntity<ApiResponse<RoleResponse>> createRole(
      @Valid @RequestBody CreateRoleRequest request) {
    log.info("API POST /admin/roles - Bắt đầu tạo vai trò: {}", request.getName());
    RoleResponse response = roleService.createRole(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo vai trò thành công", response));
  }

  /**
   * Lấy toàn bộ danh sách vai trò hiện có trong hệ thống.
   *
   * @return ResponseEntity chứa danh sách DTO vai trò
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
    log.info("API GET /admin/roles - Truy vấn toàn bộ danh sách vai trò");
    List<RoleResponse> response = roleService.getAllRoles();
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * Gán danh sách quyền hạn cụ thể cho vai trò.
   *
   * @param id ID của vai trò cần gán quyền
   * @param permissionNames Danh sách tên các quyền hạn mong muốn
   * @return ResponseEntity chứa DTO phản hồi vai trò sau cập nhật
   */
  @PostMapping("/{id}/permissions")
  public ResponseEntity<ApiResponse<RoleResponse>> assignPermissions(
      @PathVariable Long id, @RequestBody Set<String> permissionNames) {
    log.info("API POST /admin/roles/{}/permissions - Gán các quyền hạn: {}", id, permissionNames);
    RoleResponse response = roleService.assignPermissionsToRole(id, permissionNames);
    return ResponseEntity.ok(ApiResponse.success("Gán quyền hạn thành công", response));
  }
}
