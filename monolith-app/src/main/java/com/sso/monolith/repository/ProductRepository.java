package com.sso.monolith.repository;

import com.sso.monolith.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository xử lý các truy vấn cơ sở dữ liệu cho thực thể {@link Product}.
 *
 * @author SSO Platform Team
 * @since Sprint 07
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

  /** Tìm kiếm các sản phẩm đang active phục vụ hiển thị công khai. */
  Page<Product> findByActiveTrue(Pageable pageable);
}
