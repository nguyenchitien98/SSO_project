package com.sso.monolith.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.monolith.dto.request.CreateProductRequest;
import com.sso.monolith.dto.request.UpdateProductRequest;
import com.sso.monolith.dto.response.ProductResponse;
import com.sso.monolith.service.ProductService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cung cấp các endpoints quản lý Sản phẩm (Product) trên Monolith.
 *
 * <p>Tại sao Controller này không có @PreAuthorize? - Theo thiết kế phân quyền tập trung tại
 * Service Layer, tầng Controller đóng vai trò định tuyến (routing) và chuyển giao (delegation) các
 * yêu cầu bảo mật trực tiếp xuống Service Layer xử lý. - Điều này giúp tách biệt tối đa logic giao
 * thức mạng và logic nghiệp vụ bảo mật cốt lõi.
 *
 * @author SSO Platform Team
 * @since Sprint 07
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

  private final ProductService productService;

  /** API phân trang danh sách toàn bộ sản phẩm. */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(Pageable pageable) {
    log.info("API GET /api/products - Lấy danh sách sản phẩm phân trang");
    Page<ProductResponse> products = productService.getProducts(pageable);
    return ResponseEntity.ok(ApiResponse.success(products));
  }

  /** API chi tiết sản phẩm theo ID. */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
    log.info("API GET /api/products/{} - Lấy chi tiết sản phẩm", id);
    ProductResponse product = productService.getProductById(id);
    return ResponseEntity.ok(ApiResponse.success(product));
  }

  /**
   * API tạo mới sản phẩm.
   *
   * <p>Đọc UUID của người dùng hiện tại đang đăng nhập từ `sub` claim của Access Token JWT.
   */
  @PostMapping
  public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateProductRequest request) {

    UUID creatorId = UUID.fromString(jwt.getSubject());
    log.info("API POST /api/products - Yêu cầu tạo sản phẩm bởi user UUID: {}", creatorId);

    ProductResponse response = productService.createProduct(request, creatorId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo sản phẩm thành công", response));
  }

  /** API cập nhật thông tin sản phẩm. */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
      @PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {

    log.info("API PUT /api/products/{} - Yêu cầu cập nhật sản phẩm", id);
    ProductResponse response = productService.updateProduct(id, request);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", response));
  }

  /** API xóa vật lý sản phẩm khỏi hệ thống. */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
    log.warn("API DELETE /api/products/{} - Yêu cầu xóa sản phẩm", id);
    productService.deleteProduct(id);
    return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
  }
}
