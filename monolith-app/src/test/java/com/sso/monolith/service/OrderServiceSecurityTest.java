package com.sso.monolith.service;

import static org.junit.jupiter.api.Assertions.*;

import com.sso.monolith.entity.Order;
import com.sso.monolith.entity.Product;
import com.sso.monolith.entity.UserProfile;
import com.sso.monolith.repository.AuditLogRepository;
import com.sso.monolith.repository.OrderRepository;
import com.sso.monolith.repository.PaymentRepository;
import com.sso.monolith.repository.ProductRepository;
import com.sso.monolith.repository.UserProfileRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

/**
 * Kiểm thử tích hợp bảo mật (Security Integration Test) cho {@link OrderService} liên quan tới sở
 * hữu tài nguyên (ABAC).
 *
 * <p>Kiểm chứng các kịch bản: - Người dùng chỉ được xem/hủy đơn hàng của chính mình (Owner check).
 * - Quản trị viên (ADMIN) được quyền xem/hủy đơn hàng của người dùng khác. - Người dùng khác xem
 * đơn hàng của owner sẽ bị ném {@link AccessDeniedException}.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceSecurityTest {

  @Autowired private OrderService orderService;

  @Autowired private OrderRepository orderRepository;

  @Autowired private UserProfileRepository userProfileRepository;

  @Autowired private ProductRepository productRepository;

  @Autowired private PaymentRepository paymentRepository;

  @Autowired private AuditLogRepository auditLogRepository;

  @MockBean
  private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
      clientRegistrationRepository;

  @MockBean private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

  private UUID userAId;
  private UUID userBId;
  private Order orderA;

  @BeforeEach
  void setUp() {
    // Xóa sạch dữ liệu DB in-memory H2 theo thứ tự đúng ràng buộc khóa ngoại
    auditLogRepository.deleteAll();
    paymentRepository.deleteAll();
    orderRepository.deleteAll();
    productRepository.deleteAll();
    userProfileRepository.deleteAll();

    userAId = UUID.randomUUID();
    userBId = UUID.randomUUID();

    // 1. Tạo và lưu hồ sơ người dùng
    UserProfile userA =
        UserProfile.builder().id(userAId).displayName("User A").preferences("{}").build();
    userProfileRepository.save(userA);

    UserProfile userB =
        UserProfile.builder().id(userBId).displayName("User B").preferences("{}").build();
    userProfileRepository.save(userB);

    // 2. Tạo sản phẩm mẫu
    Product product =
        Product.builder()
            .name("Order Test Product")
            .price(BigDecimal.valueOf(50.0))
            .stock(100)
            .createdBy(userA)
            .build();
    productRepository.save(product);

    // 3. Tạo và lưu đơn hàng của User A
    orderA =
        Order.builder()
            .user(userA)
            .orderCode("ORD-TEST-A")
            .status("PENDING")
            .totalAmount(BigDecimal.valueOf(50.0))
            .shippingAddress("123 Street")
            .build();
    orderA = orderRepository.save(orderA);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Helper thiết lập Token JWT giả lập vào Security Context của Spring Security. */
  private void mockJwtAuthentication(UUID userId, String... authorities) {
    Jwt jwt =
        Jwt.withTokenValue("mock-jwt-token-value")
            .header("alg", "none")
            .claim("sub", userId.toString())
            .build();

    List<GrantedAuthority> authorityList =
        Arrays.stream(authorities).map(SimpleGrantedAuthority::new).collect(Collectors.toList());

    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, authorityList);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  /** Test: Người sở hữu (User A) truy cập đơn hàng của mình -> Thành công. */
  @Test
  void getOrder_AsOwner_Success() {
    mockJwtAuthentication(userAId, "ORDER_READ");

    assertDoesNotThrow(
        () -> {
          orderService.getOrderById(orderA.getId());
        });
  }

  /**
   * Test: Người dùng khác (User B) truy cập đơn hàng của User A -> Bị chặn ném
   * AccessDeniedException.
   */
  @Test
  void getOrder_AsNonOwner_ThrowsAccessDenied() {
    mockJwtAuthentication(userBId, "ORDER_READ");

    assertThrows(
        AccessDeniedException.class,
        () -> {
          orderService.getOrderById(orderA.getId());
        });
  }

  /** Test: Quản trị viên (ADMIN) truy cập đơn hàng của User A -> Thành công. */
  @Test
  void getOrder_AsAdmin_Success() {
    // ADMIN có ROLE_ADMIN và quyền đọc đơn hàng
    mockJwtAuthentication(UUID.randomUUID(), "ROLE_ADMIN", "ORDER_READ");

    assertDoesNotThrow(
        () -> {
          orderService.getOrderById(orderA.getId());
        });
  }

  /** Test: Người sở hữu (User A) hủy đơn hàng của mình -> Thành công. */
  @Test
  void cancelOrder_AsOwner_Success() {
    mockJwtAuthentication(userAId, "ORDER_READ"); // Cần có authentication sở hữu

    assertDoesNotThrow(
        () -> {
          orderService.cancelOrder(orderA.getId());
        });
  }

  /**
   * Test: Người dùng khác (User B) hủy đơn hàng của User A -> Bị chặn ném AccessDeniedException.
   */
  @Test
  void cancelOrder_AsNonOwner_ThrowsAccessDenied() {
    mockJwtAuthentication(userBId, "ORDER_READ");

    assertThrows(
        AccessDeniedException.class,
        () -> {
          orderService.cancelOrder(orderA.getId());
        });
  }
}
