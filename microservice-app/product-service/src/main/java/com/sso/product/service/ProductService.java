package com.sso.product.service;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.product.entity.Product;
import com.sso.product.repository.ProductRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp dịch vụ quản lý thông tin sản phẩm (Product Service).
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;

  @Transactional(readOnly = true)
  public Page<Product> getAllActiveProducts(Pageable pageable) {
    return productRepository.findAllByActiveTrue(pageable);
  }

  @Transactional(readOnly = true)
  public Product getActiveProductById(Long id) {
    return productRepository
        .findByIdAndActiveTrue(id)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm hoặc sản phẩm đã bị xóa"));
  }

  @Transactional
  public Product createProduct(Product product, UUID createdBy) {
    product.setCreatedBy(createdBy);
    product.setActive(true);
    product.setCreatedAt(Instant.now());
    return productRepository.save(product);
  }

  @Transactional
  public Product updateProduct(Long id, Product updateReq) {
    Product product = getActiveProductById(id);

    product.setName(updateReq.getName());
    product.setDescription(updateReq.getDescription());
    product.setPrice(updateReq.getPrice());
    product.setStock(updateReq.getStock());
    if (updateReq.getCategory() != null) {
      product.setCategory(updateReq.getCategory());
    }

    return productRepository.save(product);
  }

  @Transactional
  public void softDeleteProduct(Long id) {
    Product product = getActiveProductById(id);
    product.setActive(false); // Soft Delete
    productRepository.save(product);
  }
}
