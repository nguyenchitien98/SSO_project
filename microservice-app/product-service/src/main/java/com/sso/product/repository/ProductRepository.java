package com.sso.product.repository;

import com.sso.product.entity.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository quản lý thực thể Product.
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

  Page<Product> findAllByActiveTrue(Pageable pageable);

  Optional<Product> findByIdAndActiveTrue(Long id);
}
