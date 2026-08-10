package com.sso.monolith.service;

import static org.junit.jupiter.api.Assertions.*;

import com.sso.monolith.dto.request.CreateProductRequest;
import com.sso.monolith.entity.UserProfile;
import com.sso.monolith.repository.AuditLogRepository;
import com.sso.monolith.repository.OrderRepository;
import com.sso.monolith.repository.ProductRepository;
import com.sso.monolith.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Kiểm thử tích hợp bảo mật (Security Integration Test) cho {@link ProductService}.
 *
 * <p>Kiểm chứng hoạt động của cơ chế phân quyền bảo mật cấp phương thức (Method Security)
 * `@PreAuthorize`: - Người dùng chỉ có quyền đọc (PRODUCT_READ) không được phép ghi hoặc xóa. -
 * Người dùng có quyền ghi (PRODUCT_CREATE) được phép tạo sản phẩm. - Chỉ vai trò quản trị viên
 * (ROLE_ADMIN hoặc ROLE_MANAGER) mới được phép xóa sản phẩm.
 *
 * @author SSO Platform Team
 * @since Sprint 07
 */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class ProductServiceSecurityTest {

  @Autowired private ProductService productService;
  @Autowired private ProductRepository productRepository;
  @Autowired private UserProfileRepository userProfileRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private AuditLogRepository auditLogRepository;

  @MockBean
  private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
      clientRegistrationRepository;

  @MockBean private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

  private CreateProductRequest createRequest;
  private UUID creatorId;
  private UserProfile mockCreator;

  @BeforeEach
  void setUp() {
    auditLogRepository.deleteAll();
    orderRepository.deleteAll();
    productRepository.deleteAll();
    userProfileRepository.deleteAll();

    creatorId = UUID.randomUUID();
    createRequest =
        CreateProductRequest.builder()
            .name("Test Product")
            .description("Sample description")
            .price(BigDecimal.valueOf(100.0))
            .stock(10)
            .category("Electronics")
            .build();

    mockCreator =
        UserProfile.builder().id(creatorId).displayName("Test Creator").preferences("{}").build();

    userProfileRepository.save(mockCreator);
  }

  /** Test: Người dùng có quyền PRODUCT_READ xem danh sách sản phẩm thành công. */
  @Test
  @WithMockUser(authorities = "PRODUCT_READ")
  void getProducts_WithProductRead_Success() {
    assertDoesNotThrow(
        () -> {
          productService.getProducts(PageRequest.of(0, 10));
        });
  }

  /**
   * Test: Người dùng chỉ có quyền PRODUCT_READ gọi tạo sản phẩm -> Bị chặn ném
   * AccessDeniedException.
   */
  @Test
  @WithMockUser(authorities = "PRODUCT_READ")
  void createProduct_WithProductRead_ThrowsAccessDenied() {
    assertThrows(
        AccessDeniedException.class,
        () -> {
          productService.createProduct(createRequest, creatorId);
        });
  }

  /** Test: Người dùng có quyền PRODUCT_CREATE gọi tạo sản phẩm -> Thành công. */
  @Test
  @WithMockUser(authorities = "PRODUCT_CREATE")
  void createProduct_WithProductCreate_Success() {
    assertDoesNotThrow(
        () -> {
          productService.createProduct(createRequest, creatorId);
        });
  }

  /**
   * Test: Người dùng có vai trò thông thường (ROLE_USER) gọi xóa sản phẩm -> Bị chặn ném
   * AccessDeniedException.
   */
  @Test
  @WithMockUser(roles = "USER")
  void deleteProduct_WithUserRole_ThrowsAccessDenied() {
    assertThrows(
        AccessDeniedException.class,
        () -> {
          productService.deleteProduct(999L);
        });
  }

  /** Test: Quản trị viên có vai trò ROLE_ADMIN gọi xóa sản phẩm -> Thành công. */
  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteProduct_WithAdminRole_Success() {
    com.sso.monolith.entity.Product product =
        com.sso.monolith.entity.Product.builder()
            .name("Delete Me")
            .price(BigDecimal.valueOf(10.0))
            .stock(5)
            .createdBy(mockCreator)
            .build();
    product = productRepository.save(product);
    final Long productId = product.getId();

    assertDoesNotThrow(
        () -> {
          productService.deleteProduct(productId);
        });
  }
}
