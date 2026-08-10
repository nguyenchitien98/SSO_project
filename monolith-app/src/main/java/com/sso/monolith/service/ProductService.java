package com.sso.monolith.service;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.monolith.dto.request.CreateProductRequest;
import com.sso.monolith.dto.request.UpdateProductRequest;
import com.sso.monolith.dto.response.ProductResponse;
import com.sso.monolith.entity.Product;
import com.sso.monolith.entity.UserProfile;
import com.sso.monolith.repository.ProductRepository;
import com.sso.monolith.repository.UserProfileRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp dịch vụ (Service) xử lý các nghiệp vụ liên quan tới Sản phẩm (Product).
 *
 * <p>Tại sao áp dụng bảo mật @PreAuthorize ở Service Layer? - Phân quyền tại Service Layer là chốt
 * chặn bảo mật cuối cùng trước khi thay đổi dữ liệu, ngăn ngừa các cuộc gọi bỏ qua Controller hoặc
 * gọi chéo nội bộ bất hợp pháp. - Cho phép phân định rõ ràng quyền thao tác: đọc (PRODUCT_READ),
 * ghi (PRODUCT_CREATE), cập nhật (PRODUCT_UPDATE), và xóa (ADMIN hoặc MANAGER).
 *
 * @author SSO Platform Team
 * @since Sprint 07
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductService {

  private final ProductRepository productRepository;
  private final UserProfileRepository userProfileRepository;

  /**
   * Lấy danh sách sản phẩm phân trang.
   *
   * @param pageable Tham số phân trang
   * @return Trang danh sách DTO sản phẩm
   */
  @PreAuthorize("hasAuthority('PRODUCT_READ')")
  public Page<ProductResponse> getProducts(Pageable pageable) {
    log.info("Thực hiện lấy danh sách sản phẩm phân trang");
    return productRepository.findAll(pageable).map(this::mapToProductResponse);
  }

  /**
   * Lấy thông tin chi tiết một sản phẩm theo ID.
   *
   * @param id ID của sản phẩm
   * @return DTO sản phẩm
   */
  @PreAuthorize("hasAuthority('PRODUCT_READ')")
  public ProductResponse getProductById(Long id) {
    log.info("Thực hiện lấy chi tiết sản phẩm ID: {}", id);
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + id));
    return mapToProductResponse(product);
  }

  /**
   * Tạo mới một sản phẩm trong hệ thống.
   *
   * @param request DTO chứa thông tin sản phẩm cần tạo
   * @param creatorId UUID của tài khoản người tạo lấy từ SSO chéo
   * @return DTO sản phẩm sau khi lưu DB thành công
   */
  @Transactional
  @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
  public ProductResponse createProduct(CreateProductRequest request, UUID creatorId) {
    log.info("Thực hiện tạo sản phẩm mới chéo bởi user UUID: {}", creatorId);

    UserProfile creator =
        userProfileRepository
            .findById(creatorId)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người tạo"));

    Product product =
        Product.builder()
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .stock(request.getStock())
            .category(request.getCategory())
            .imageUrl(request.getImageUrl())
            .active(true)
            .createdBy(creator)
            .build();

    Product saved = productRepository.save(product);
    log.info("Tạo sản phẩm thành công, ID: {}", saved.getId());
    return mapToProductResponse(saved);
  }

  /**
   * Cập nhật thông tin chi tiết sản phẩm.
   *
   * @param id ID sản phẩm cần cập nhật
   * @param request DTO chứa thông tin mới
   * @return DTO sản phẩm sau cập nhật
   */
  @Transactional
  @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
  public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
    log.info("Thực hiện cập nhật sản phẩm ID: {}", id);
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + id));

    product.setName(request.getName());
    product.setDescription(request.getDescription());
    product.setPrice(request.getPrice());
    product.setStock(request.getStock());
    product.setCategory(request.getCategory());
    product.setImageUrl(request.getImageUrl());
    product.setActive(request.getActive());
    product.setUpdatedAt(Instant.now());

    Product saved = productRepository.save(product);
    log.info("Cập nhật sản phẩm thành công cho ID: {}", saved.getId());
    return mapToProductResponse(saved);
  }

  /**
   * Xóa một sản phẩm khỏi hệ thống.
   *
   * <p>Tại sao kiểm tra Role chéo chắt chẽ? - Chỉ người có vai trò ADMIN hoặc MANAGER mới được phép
   * xóa sản phẩm vật lý ra khỏi DB.
   *
   * @param id ID của sản phẩm cần xóa
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
  public void deleteProduct(Long id) {
    log.warn("Yêu cầu xóa sản phẩm ID: {}", id);
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + id));

    productRepository.delete(product);
    log.info("Xóa thành công sản phẩm ID: {}", id);
  }

  /** Chuyển đổi từ thực thể {@link Product} sang DTO phản hồi {@link ProductResponse}. */
  private ProductResponse mapToProductResponse(Product product) {
    return ProductResponse.builder()
        .id(product.getId())
        .name(product.getName())
        .description(product.getDescription())
        .price(product.getPrice())
        .stock(product.getStock())
        .category(product.getCategory())
        .imageUrl(product.getImageUrl())
        .active(product.getActive())
        .createdBy(product.getCreatedBy().getId())
        .createdAt(product.getCreatedAt())
        .updatedAt(product.getUpdatedAt())
        .build();
  }
}
