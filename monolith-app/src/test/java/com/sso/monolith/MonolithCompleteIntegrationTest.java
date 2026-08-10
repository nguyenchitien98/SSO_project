package com.sso.monolith;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sso.monolith.dto.request.CreateOrderRequest;
import com.sso.monolith.entity.Order;
import com.sso.monolith.entity.Product;
import com.sso.monolith.entity.UserProfile;
import com.sso.monolith.repository.AuditLogRepository;
import com.sso.monolith.repository.OrderRepository;
import com.sso.monolith.repository.PaymentRepository;
import com.sso.monolith.repository.ProductRepository;
import com.sso.monolith.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Bộ kiểm thử tích hợp toàn diện End-to-End (Complete Integration Test Suite) cho Monolith App.
 *
 * <p>Kiểm chứng các kịch bản quan trọng: - Scenario 1 — Happy path: LOGIN (JWT) ➔ GET /products ➔
 * POST /orders ➔ GET /orders/{id} - Scenario 2 — Auth failure: Expired JWT ➔ Trả về HTTP 401
 * Unauthorized - Scenario 3 — Authorization failure: Tài khoản role thường ➔ Yêu cầu xóa sản phẩm ➔
 * Trả về HTTP 403 Forbidden - Scenario 4 — Ownership violation: Người dùng thường ➔ Xem đơn hàng
 * của người khác ➔ Trả về HTTP 403 Forbidden
 *
 * @author SSO Platform Team
 * @since Sprint 10
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MonolithCompleteIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private AuditLogRepository auditLogRepository;

  @Autowired private PaymentRepository paymentRepository;

  @Autowired private OrderRepository orderRepository;

  @Autowired private ProductRepository productRepository;

  @Autowired private UserProfileRepository userProfileRepository;

  @MockBean
  private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
      clientRegistrationRepository;

  @MockBean private JwtDecoder jwtDecoder;

  private UUID userAId;
  private UUID userBId;
  private UserProfile userProfileA;
  private UserProfile userProfileB;
  private Product testProduct;
  private Order orderB;

  @BeforeEach
  void setUp() {
    // Dọn dẹp DB đúng thứ tự ràng buộc khóa ngoại chéo
    auditLogRepository.deleteAll();
    paymentRepository.deleteAll();
    orderRepository.deleteAll();
    productRepository.deleteAll();
    userProfileRepository.deleteAll();

    userAId = UUID.randomUUID();
    userBId = UUID.randomUUID();

    // 1. Tạo người dùng A và B
    userProfileA =
        UserProfile.builder().id(userAId).displayName("User A").preferences("{}").build();
    userProfileRepository.save(userProfileA);

    userProfileB =
        UserProfile.builder().id(userBId).displayName("User B").preferences("{}").build();
    userProfileRepository.save(userProfileB);

    // 2. Tạo sản phẩm
    testProduct =
        Product.builder()
            .name("E2E Integration Test Product")
            .price(BigDecimal.valueOf(100.0))
            .stock(10)
            .active(true)
            .createdBy(userProfileB)
            .build();
    testProduct = productRepository.save(testProduct);

    // 3. Tạo đơn hàng của user B
    orderB =
        Order.builder()
            .user(userProfileB)
            .orderCode("ORD-B-E2E")
            .status("PENDING")
            .totalAmount(BigDecimal.valueOf(100.0))
            .shippingAddress("User B Address")
            .build();
    orderB = orderRepository.save(orderB);
  }

  private Jwt createMockJwt(
      UUID userId, String email, List<String> roles, List<String> permissions) {
    return Jwt.withTokenValue("mock-token")
        .header("alg", "none")
        .claim("sub", userId.toString())
        .claim("email", email)
        .claim("roles", roles)
        .claim("permissions", permissions)
        .build();
  }

  /**
   * Scenario 1 — Happy path: Đăng nhập hợp lệ ➔ Lấy sản phẩm ➔ Tạo đơn hàng ➔ Lấy chi tiết đơn
   * hàng.
   */
  @Test
  void happyPath_E2EFlow_Success() throws Exception {
    // 1. Mock JWT Token hợp lệ cho User A
    Jwt validJwt =
        createMockJwt(
            userAId,
            "usera@sso.com",
            List.of("USER"),
            List.of("PRODUCT_READ", "ORDER_READ", "ORDER_CREATE"));
    when(jwtDecoder.decode("token-user-a")).thenReturn(validJwt);

    // 2. GET /api/products -> Xem danh sách sản phẩm
    mockMvc
        .perform(get("/api/products").header("Authorization", "Bearer token-user-a"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].name").value("E2E Integration Test Product"));

    // 3. POST /api/orders -> Thực hiện tạo đơn hàng mới mua testProduct
    CreateOrderRequest.OrderItemRequest itemReq =
        CreateOrderRequest.OrderItemRequest.builder()
            .productId(testProduct.getId())
            .quantity(2)
            .build();
    CreateOrderRequest orderReq =
        CreateOrderRequest.builder()
            .items(List.of(itemReq))
            .shippingAddress("User A Address")
            .notes("Buy products")
            .idempotencyKey("idemp-key-happy-path")
            .build();

    String responseBody =
        mockMvc
            .perform(
                post("/api/orders")
                    .header("Authorization", "Bearer token-user-a")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Trích xuất order ID từ JSON trả về
    Long createdOrderId = objectMapper.readTree(responseBody).get("data").get("id").asLong();

    // 4. GET /api/orders/{id} -> Xem chi tiết đơn hàng vừa tạo (Với tư cách chủ sở hữu User A)
    mockMvc
        .perform(
            get("/api/orders/" + createdOrderId).header("Authorization", "Bearer token-user-a"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(createdOrderId));
  }

  /** Scenario 2 — Auth failure: Gửi Token hết hạn (Expired JWT) ➔ Trả về HTTP 401. */
  @Test
  void authFailure_ExpiredJwt_Returns401() throws Exception {
    OAuth2Error error = new OAuth2Error("invalid_token", "Jwt is expired", null);
    when(jwtDecoder.decode("expired-jwt-token"))
        .thenThrow(new JwtValidationException("Jwt is expired", List.of(error)));

    mockMvc
        .perform(get("/api/products").header("Authorization", "Bearer expired-jwt-token"))
        .andExpect(status().isUnauthorized());
  }

  /**
   * Scenario 3 — Authorization failure: Người dùng thường không có quyền ADMIN ➔ Xóa sản phẩm ➔ Trả
   * về HTTP 403.
   */
  @Test
  void authorizationFailure_UserRoleDeleteProduct_Returns403() throws Exception {
    // Mock JWT cho user thường (không có role ADMIN/MANAGER)
    Jwt userJwt = createMockJwt(userAId, "usera@sso.com", List.of("USER"), List.of("PRODUCT_READ"));
    when(jwtDecoder.decode("token-user-a")).thenReturn(userJwt);

    mockMvc
        .perform(
            delete("/api/products/" + testProduct.getId())
                .header("Authorization", "Bearer token-user-a"))
        .andExpect(status().isForbidden());
  }

  /**
   * Scenario 4 — Ownership violation: Người dùng A ➔ Xem đơn hàng của Người dùng B ➔ Trả về HTTP
   * 403.
   */
  @Test
  void ownershipViolation_UserAAccessUserBOrder_Returns403() throws Exception {
    // Mock JWT cho User A
    Jwt userAJwt = createMockJwt(userAId, "usera@sso.com", List.of("USER"), List.of("ORDER_READ"));
    when(jwtDecoder.decode("token-user-a")).thenReturn(userAJwt);

    // Đơn hàng orderB thuộc sở hữu của User B. User A cố truy cập sẽ bị từ chối.
    mockMvc
        .perform(
            get("/api/orders/" + orderB.getId()).header("Authorization", "Bearer token-user-a"))
        .andExpect(status().isForbidden());
  }
}
