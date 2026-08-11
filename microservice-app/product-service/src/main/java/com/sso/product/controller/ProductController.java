package com.sso.product.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.product.entity.Product;
import com.sso.product.security.AuthorizationService;
import com.sso.product.security.CurrentUser;
import com.sso.product.security.CurrentUserResolver;
import com.sso.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller tiếp nhận các yêu cầu nghiệp vụ liên quan tới Sản phẩm (Product Controller).
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

  private final ProductService productService;
  private final CurrentUserResolver userResolver;
  private final AuthorizationService authService;

  /**
   * Xem danh sách sản phẩm phân trang (Công khai - Không cần đăng nhập).
   *
   * @param page Số trang (0-indexed)
   * @param size Kích thước trang
   * @return ResponseEntity chứa danh sách sản phẩm
   */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<Product>>> getActiveProducts(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    log.info("API GET /api/products - Phân trang page={}, size={}", page, size);
    Page<Product> products = productService.getAllActiveProducts(PageRequest.of(page, size));
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", products));
  }

  /**
   * Xem chi tiết sản phẩm theo ID (Công khai).
   *
   * @param id Mã sản phẩm
   * @return ResponseEntity chứa thông tin sản phẩm
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<?>> getProductById(@PathVariable Long id) {
    log.info("API GET /api/products/{}", id);
    Product product = productService.getActiveProductById(id);
    return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công", product));
  }

  /**
   * Thêm sản phẩm mới (Yêu cầu quyền PRODUCT_CREATE).
   *
   * @param request HTTP request chứa headers
   * @param product Thông tin sản phẩm cần tạo
   * @return ResponseEntity chứa sản phẩm đã tạo
   */
  @PostMapping
  public ResponseEntity<ApiResponse<?>> createProduct(
      HttpServletRequest request, @RequestBody Product product) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API POST /api/products - Yêu cầu từ user: {}", currentUser != null ? currentUser.email() : "GUEST");

    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }

    authService.requirePermission(currentUser, "PRODUCT_CREATE");

    Product created = productService.createProduct(product, UUID.fromString(currentUser.id()));
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Thêm sản phẩm thành công", created));
  }

  /**
   * Cập nhật thông tin sản phẩm (Yêu cầu quyền PRODUCT_UPDATE).
   *
   * @param request HTTP request
   * @param id Mã sản phẩm cần cập nhật
   * @param updateReq Dữ liệu cập nhật
   * @return ResponseEntity chứa sản phẩm đã cập nhật
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<?>> updateProduct(
      HttpServletRequest request, @PathVariable Long id, @RequestBody Product updateReq) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API PUT /api/products/{} - Yêu cầu từ user: {}", id, currentUser != null ? currentUser.email() : "GUEST");

    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }

    authService.requirePermission(currentUser, "PRODUCT_UPDATE");

    Product updated = productService.updateProduct(id, updateReq);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", updated));
  }

  /**
   * Xóa sản phẩm soft delete (Yêu cầu vai trò ADMIN hoặc MANAGER).
   *
   * @param request HTTP request
   * @param id Mã sản phẩm cần xóa
   * @return ResponseEntity chứa kết quả
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<?>> deleteProduct(
      HttpServletRequest request, @PathVariable Long id) {
    CurrentUser currentUser = userResolver.resolve(request);
    log.info("API DELETE /api/products/{} - Yêu cầu từ user: {}", id, currentUser != null ? currentUser.email() : "GUEST");

    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.UNAUTHORIZED, "Chưa xác thực"));
    }

    // Yêu cầu vai trò ADMIN hoặc MANAGER
    boolean isAdmin = currentUser.roles().contains("ADMIN");
    boolean isManager = currentUser.roles().contains("MANAGER");
    if (!isAdmin && !isManager) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(
          com.sso.common.exception.ErrorCode.FORBIDDEN, "Không có quyền thực hiện hành động này"));
    }

    productService.softDeleteProduct(id);
    return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
  }
}
