package com.sso.monolith.service;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.monolith.dto.request.CreateOrderRequest;
import com.sso.monolith.dto.response.OrderResponse;
import com.sso.monolith.entity.Order;
import com.sso.monolith.entity.OrderItem;
import com.sso.monolith.entity.Product;
import com.sso.monolith.entity.UserProfile;
import com.sso.monolith.repository.OrderRepository;
import com.sso.monolith.repository.ProductRepository;
import com.sso.monolith.repository.UserProfileRepository;
import com.sso.monolith.security.annotation.Auditable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp dịch vụ (Service) xử lý các nghiệp vụ liên quan tới Đơn hàng (Order).
 *
 * <p>Tại sao áp dụng phân quyền dựa trên quyền sở hữu tài nguyên (ABAC)? - Một người dùng thông
 * thường chỉ được xem chi tiết đơn hàng hoặc hủy đơn hàng của CHÍNH MÌNH. - Quản trị viên (ADMIN)
 * được xem và xử lý đơn hàng của toàn bộ hệ thống. - Việc này được thực thi động chéo thông qua
 * `@PreAuthorize("@orderSecurity.isOwnerOrAdmin(authentication, #orderId)")`.
 *
 * @author SSO Platform Team
 * @since Sprint 08
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderService {

  private final OrderRepository orderRepository;
  private final ProductRepository productRepository;
  private final UserProfileRepository userProfileRepository;

  /**
   * Tạo mới đơn hàng (Đặt mua sản phẩm).
   *
   * <p>Nghiệp vụ bao gồm: - Kiểm tra Idempotency Key để chống trùng lặp request chéo. - Trừ kho sản
   * phẩm, báo lỗi nếu không đủ hàng tồn. - Tính toán tổng tiền và sinh mã đơn hàng tự động.
   *
   * @param request DTO yêu cầu tạo đơn hàng
   * @param userId UUID người mua hàng từ SSO Server
   * @return DTO đơn hàng đã lưu DB
   */
  @Transactional
  @PreAuthorize("hasAuthority('ORDER_CREATE')")
  @Auditable(action = "ORDER_CREATE", resource = "Order")
  public OrderResponse createOrder(CreateOrderRequest request, UUID userId) {
    log.info("Bắt đầu xử lý đặt hàng cho user UUID: {}", userId);

    // 1. Kiểm tra Idempotency Key chống trùng lặp chéo
    if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
      if (orderRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
        log.warn(
            "Phát hiện request trùng lặp với Idempotency Key: {}", request.getIdempotencyKey());
        // Tìm lại đơn hàng đã xử lý thành công trước đó và trả về
        Order existingOrder =
            orderRepository
                .findByOrderCode("ORD-" + request.getIdempotencyKey())
                .orElseThrow(
                    () ->
                        new BusinessException(
                            ErrorCode.INVALID_INPUT, "Yêu cầu trùng lặp đang được xử lý"));
        return mapToOrderResponse(existingOrder);
      }
    }

    UserProfile user =
        userProfileRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người dùng mua hàng"));

    BigDecimal totalAmount = BigDecimal.ZERO;
    List<OrderItem> orderItems = new ArrayList<>();

    Order order =
        Order.builder()
            .user(user)
            .orderCode(
                "ORD-"
                    + (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
                        ? request.getIdempotencyKey()
                        : System.currentTimeMillis() + "-" + (int) (Math.random() * 1000)))
            .status("PENDING")
            .shippingAddress(request.getShippingAddress())
            .notes(request.getNotes())
            .idempotencyKey(request.getIdempotencyKey())
            .build();

    // 2. Duyệt qua giỏ hàng sản phẩm
    for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
      Product product =
          productRepository
              .findById(itemReq.getProductId())
              .orElseThrow(
                  () ->
                      new BusinessException(
                          ErrorCode.NOT_FOUND,
                          "Không tìm thấy sản phẩm ID: " + itemReq.getProductId()));

      if (!product.getActive()) {
        throw new BusinessException(
            ErrorCode.INVALID_INPUT, "Sản phẩm " + product.getName() + " đã ngừng hoạt động");
      }

      if (product.getStock() < itemReq.getQuantity()) {
        log.warn(
            "Sản phẩm {} không đủ hàng tồn kho. Yêu cầu: {}, Tồn kho: {}",
            product.getName(),
            itemReq.getQuantity(),
            product.getStock());
        throw new BusinessException(
            ErrorCode.INVALID_INPUT, "Sản phẩm " + product.getName() + " không đủ hàng tồn kho");
      }

      // 3. Trừ kho sản phẩm vật lý
      product.setStock(product.getStock() - itemReq.getQuantity());
      productRepository.save(product);

      BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
      totalAmount = totalAmount.add(subtotal);

      OrderItem orderItem =
          OrderItem.builder()
              .product(product)
              .productName(product.getName())
              .quantity(itemReq.getQuantity())
              .unitPrice(product.getPrice())
              .subtotal(subtotal)
              .build();

      order.addItem(orderItem);
    }

    order.setTotalAmount(totalAmount);
    Order savedOrder = orderRepository.save(order);
    log.info(
        "Đặt hàng thành công. Mã đơn: {}, Tổng tiền: {}",
        savedOrder.getOrderCode(),
        savedOrder.getTotalAmount());
    return mapToOrderResponse(savedOrder);
  }

  /**
   * Xem thông tin chi tiết một đơn hàng theo ID.
   *
   * <p>Quy trình bảo mật: - Phải có quyền `ORDER_READ`. - Phải vượt qua kiểm tra ownership: chỉ xem
   * được đơn hàng của chính mình (hoặc là ADMIN).
   */
  @PreAuthorize(
      "hasAuthority('ORDER_READ') and @orderSecurity.isOwnerOrAdmin(authentication, #orderId)")
  public OrderResponse getOrderById(@P("orderId") Long orderId) {
    log.info("Thực hiện lấy chi tiết đơn hàng ID: {}", orderId);
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy đơn hàng ID: " + orderId));
    return mapToOrderResponse(order);
  }

  /** Lấy danh sách đơn hàng chéo (Phân quyền: ADMIN xem tất cả, USER chỉ xem của mình). */
  public Page<Order> getMyOrders(UUID userId, boolean isAdmin, Pageable pageable) {
    if (isAdmin) {
      log.info("ADMIN truy vấn tất cả đơn hàng hệ thống");
      return orderRepository.findAll(pageable);
    } else {
      log.info("USER {} truy vấn danh sách đơn hàng cá nhân", userId);
      return orderRepository.findByUser_Id(userId, pageable);
    }
  }

  /**
   * Hủy đơn hàng.
   *
   * <p>Quy trình bảo mật: - Chỉ người sở hữu đơn hàng (owner) hoặc ADMIN mới được phép hủy. - Hoàn
   * trả tồn kho cho các sản phẩm trong đơn hàng.
   */
  @Transactional
  @PreAuthorize("@orderSecurity.isOwnerOrAdmin(authentication, #orderId)")
  @Auditable(action = "ORDER_CANCEL", resource = "Order")
  public OrderResponse cancelOrder(@P("orderId") Long orderId) {
    log.warn("Thực hiện hủy đơn hàng ID: {}", orderId);
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.NOT_FOUND, "Không tìm thấy đơn hàng ID: " + orderId));

    if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT, "Không thể hủy đơn hàng ở trạng thái: " + order.getStatus());
    }

    order.setStatus("CANCELLED");
    order.setUpdatedAt(Instant.now());

    // Hoàn trả tồn kho cho các sản phẩm
    for (OrderItem item : order.getItems()) {
      Product product = item.getProduct();
      product.setStock(product.getStock() + item.getQuantity());
      productRepository.save(product);
    }

    Order saved = orderRepository.save(order);
    log.info("Hủy đơn hàng thành công, Mã đơn: {}", saved.getOrderCode());
    return mapToOrderResponse(saved);
  }

  /** Map từ thực thể Order sang DTO phản hồi. */
  private OrderResponse mapToOrderResponse(Order order) {
    List<OrderResponse.OrderItemResponse> items =
        order.getItems().stream()
            .map(
                item ->
                    OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
            .toList();

    return OrderResponse.builder()
        .id(order.getId())
        .userId(order.getUser().getId())
        .orderCode(order.getOrderCode())
        .status(order.getStatus())
        .totalAmount(order.getTotalAmount())
        .shippingAddress(order.getShippingAddress())
        .notes(order.getNotes())
        .createdAt(order.getCreatedAt())
        .updatedAt(order.getUpdatedAt())
        .items(items)
        .build();
  }
}
