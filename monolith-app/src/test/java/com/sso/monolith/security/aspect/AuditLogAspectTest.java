package com.sso.monolith.security.aspect;

import static org.junit.jupiter.api.Assertions.*;

import com.sso.monolith.dto.request.CreateProductRequest;
import com.sso.monolith.entity.AuditLog;
import com.sso.monolith.entity.UserProfile;
import com.sso.monolith.repository.AuditLogRepository;
import com.sso.monolith.repository.OrderRepository;
import com.sso.monolith.repository.PaymentRepository;
import com.sso.monolith.repository.ProductRepository;
import com.sso.monolith.repository.UserProfileRepository;
import com.sso.monolith.service.ProductService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

/**
 * Kiểm thử tích hợp bảo mật cho aspect {@link AuditLogAspect} (AOP).
 *
 * <p>Kiểm chứng rằng khi một phương thức gắn `@Auditable` được thực thi dưới một phiên xác thực hợp
 * lệ, hệ thống AOP sẽ tự động bắt khía cạnh và lưu lại 1 dòng Audit Log vào CSDL.
 *
 * @author SSO Platform Team
 * @since Sprint 09
 */
@SpringBootTest
@ActiveProfiles("test")
class AuditLogAspectTest {

  @Autowired private ProductService productService;

  @Autowired private AuditLogRepository auditLogRepository;

  @Autowired private UserProfileRepository userProfileRepository;

  @Autowired private ProductRepository productRepository;

  @Autowired private OrderRepository orderRepository;

  @Autowired private PaymentRepository paymentRepository;

  @MockBean
  private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
      clientRegistrationRepository;

  @MockBean private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

  private UUID actorId;
  private UserProfile actor;

  @BeforeEach
  void setUp() {
    // Dọn dẹp DB in-memory H2 đúng thứ tự ràng buộc FK
    paymentRepository.deleteAll();
    orderRepository.deleteAll();
    productRepository.deleteAll();
    auditLogRepository.deleteAll();
    userProfileRepository.deleteAll();

    actorId = UUID.randomUUID();
    actor = UserProfile.builder().id(actorId).displayName("Audit Actor").preferences("{}").build();
    userProfileRepository.save(actor);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void mockJwtAuthentication(UUID userId, String email, String... authorities) {
    Jwt jwt =
        Jwt.withTokenValue("mock-jwt-token")
            .header("alg", "none")
            .claim("sub", userId.toString())
            .claim("email", email)
            .build();

    List<GrantedAuthority> authorityList =
        Arrays.stream(authorities).map(SimpleGrantedAuthority::new).collect(Collectors.toList());

    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, authorityList);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  /** Test: Khi tạo sản phẩm thành công, AOP Aspect phải tự động lưu 1 dòng audit log. */
  @Test
  void createProduct_TriggersAuditLogAspect_Success() {
    // Arrange
    mockJwtAuthentication(actorId, "actor@sso.com", "PRODUCT_CREATE");
    CreateProductRequest request =
        CreateProductRequest.builder()
            .name("Auditable Product")
            .price(BigDecimal.valueOf(10.0))
            .stock(5)
            .build();

    // Act
    productService.createProduct(request, actorId);

    // Assert
    List<AuditLog> logs = auditLogRepository.findAll();
    assertEquals(1, logs.size(), "Phải tự động lưu 1 bản ghi audit log trong DB");

    AuditLog auditLog = logs.get(0);
    assertEquals("PRODUCT_CREATE", auditLog.getAction());
    assertEquals("Product", auditLog.getEntityType());
    assertEquals("actor@sso.com", auditLog.getActorEmail());
    assertEquals(actorId, auditLog.getActor().getId());
    assertTrue(auditLog.getNewValues().contains("SUCCESS"));
  }

  /**
   * Test: Khi gọi phương thức nhưng bị AccessDeniedException, AOP Aspect vẫn ghi nhận trạng thái
   * FAILED.
   */
  @Test
  void deleteProduct_Denied_TriggersAuditLogAspect_Failed() {
    // Arrange: Người dùng thường không có quyền xóa sản phẩm
    mockJwtAuthentication(actorId, "user@sso.com", "PRODUCT_READ");

    // Act & Assert
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> {
          productService.deleteProduct(1L);
        });

    // Aspect log failed
    List<AuditLog> logs = auditLogRepository.findAll();
    assertEquals(1, logs.size(), "Ngay cả khi lỗi bảo mật, aspect vẫn lưu audit log báo thất bại");

    AuditLog auditLog = logs.get(0);
    assertEquals("PRODUCT_DELETE", auditLog.getAction());
    assertEquals("Product", auditLog.getEntityType());
    assertTrue(auditLog.getNewValues().contains("FAILED"));
  }
}
