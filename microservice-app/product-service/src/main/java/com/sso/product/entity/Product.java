package com.sso.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thực thể Sản phẩm (Product Entity).
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "price", nullable = false, precision = 18, scale = 2)
  private BigDecimal price;

  @Column(name = "stock", nullable = false)
  @Builder.Default
  private Integer stock = 0;

  @ManyToOne
  @JoinColumn(name = "category_id")
  private Category category;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Version
  @Column(name = "version", nullable = false)
  @Builder.Default
  private Integer version = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();
}
