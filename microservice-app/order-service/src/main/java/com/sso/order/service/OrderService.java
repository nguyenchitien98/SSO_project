package com.sso.order.service;

import com.sso.common.dto.ApiResponse;
import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.order.client.PaymentClient;
import com.sso.order.dto.PaymentRequestDto;
import com.sso.order.client.ProductClient;
import com.sso.order.dto.CreateOrderRequest;
import com.sso.order.dto.ProductDto;
import com.sso.order.entity.Order;
import com.sso.order.entity.OrderItem;
import com.sso.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp dịch vụ quản lý thông tin đơn hàng (Order Service).
 *
 * @author SSO Platform Team
 * @since Sprint 14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private final OrderRepository orderRepository;
  private final ProductClient productClient;
  private final PaymentClient paymentClient;
  private final StringRedisTemplate redisTemplate;

  private static final String IDEMPOTENCY_PREFIX = "idempotency:order:";

  /**
   * Tạo đơn hàng mới kèm kiểm soát trùng lặp Idempotency-Key thông qua Redis.
   *
   * @param req Dữ liệu tạo đơn
   * @param idempotencyKey Khóa duy nhất gửi từ client
   * @param userId UUID của người dùng tạo đơn
   * @return Đối tượng Order vừa tạo
   */
  @Transactional
  public Order createOrder(CreateOrderRequest req, String idempotencyKey, UUID userId) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "Idempotency-Key không được để trống");
    }

    String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;

    // Sử dụng cơ chế setIfAbsent (SETNX) của Redis để giữ chỗ khóa trong 24 giờ
    Boolean isAbsent = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", 24, TimeUnit.HOURS);
    if (Boolean.FALSE.equals(isAbsent)) {
      log.warn("Trùng lặp Idempotency-Key phát hiện: {}", idempotencyKey);
      throw new BusinessException(ErrorCode.CONFLICT, "Yêu cầu trùng lặp (Idempotency Key đã được xử lý)");
    }

    try {
      if (req.items() == null || req.items().isEmpty()) {
        throw new BusinessException(ErrorCode.INVALID_INPUT, "Đơn hàng phải chứa ít nhất một sản phẩm");
      }

      Order order = Order.builder()
          .userId(userId)
          .orderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
          .idempotencyKey(idempotencyKey)
          .status("PENDING")
          .totalAmount(BigDecimal.ZERO)
          .createdAt(Instant.now())
          .updatedAt(Instant.now())
          .build();

      BigDecimal total = BigDecimal.ZERO;

      for (CreateOrderRequest.OrderItemDto itemDto : req.items()) {
        ProductDto product = fetchProduct(itemDto.productId());
        
        if (product.stock() < itemDto.quantity()) {
          throw new BusinessException(
              ErrorCode.INVALID_INPUT, "Sản phẩm " + product.name() + " không đủ hàng tồn kho");
        }

        BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(itemDto.quantity()));
        total = total.add(subtotal);

        OrderItem orderItem = OrderItem.builder()
            .productId(product.id())
            .productName(product.name())
            .quantity(itemDto.quantity())
            .unitPrice(product.price())
            .subtotal(subtotal)
            .build();

        order.addItem(orderItem);
      }

      order.setTotalAmount(total);
      Order savedOrder = orderRepository.save(order);

      // Cập nhật trạng thái thành công trong Redis cache
      redisTemplate.opsForValue().set(redisKey, "SUCCESS", 24, TimeUnit.HOURS);
      log.info("Đặt hàng thành công với Code: {}, Idempotency-Key: {}", savedOrder.getOrderCode(), idempotencyKey);

      // Tự động gọi sang Payment Service để xử lý thanh toán bất đồng bộ qua Feign Client
      try {
        log.info("[S2S Auth] Thực hiện thanh toán qua Feign Client cho Order ID: {}, Số tiền: {}", savedOrder.getId(), savedOrder.getTotalAmount());
        paymentClient.requestPayment(new PaymentRequestDto(
            savedOrder.getId(), savedOrder.getUserId(), savedOrder.getTotalAmount(), "E-WALLET"
        ));
      } catch (Exception payEx) {
        log.error("[S2S Auth] Không thể kết nối tới Payment Service: {}", payEx.getMessage());
        // Giữ nguyên trạng thái PENDING của đơn hàng để user thanh toán thủ công sau
      }

      return savedOrder;

    } catch (Exception e) {
      // Nếu có lỗi phát sinh trong quá trình xử lý, giải phóng khóa trong Redis để cho phép retry
      redisTemplate.delete(redisKey);
      log.error("Lỗi khi tạo đơn hàng: ", e);
      if (e instanceof BusinessException) {
        throw (BusinessException) e;
      }
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Lỗi hệ thống khi xử lý đơn hàng");
    }
  }

  /**
   * Hủy đơn hàng (Chỉ cho phép khi ở trạng thái PENDING).
   *
   * @param orderId ID đơn hàng
   * @return Đối tượng Order đã cập nhật trạng thái
   */
  @Transactional
  public Order cancelOrder(Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn hàng"));

    if (!"PENDING".equals(order.getStatus())) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT, "Không thể hủy đơn hàng đã xử lý (Trạng thái hiện tại: " + order.getStatus() + ")");
    }

    order.setStatus("CANCELLED");
    order.setUpdatedAt(Instant.now());
    return orderRepository.save(order);
  }

  @Transactional(readOnly = true)
  public Page<Order> getOrdersForUser(UUID userId, Pageable pageable) {
    return orderRepository.findAllByUserId(userId, pageable);
  }

  @Transactional(readOnly = true)
  public Page<Order> getAllOrders(Pageable pageable) {
    return orderRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Order getOrderById(Long orderId) {
    return orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn hàng"));
  }

  private ProductDto fetchProduct(Long productId) {
    try {
      ApiResponse<ProductDto> response = productClient.getProductById(productId);
      if (response == null || !response.success() || response.data() == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy thông tin sản phẩm có ID: " + productId);
      }
      return response.data();
    } catch (Exception e) {
      log.error("Không thể kết nối tới Product Service", e);
      if (e instanceof BusinessException) {
        throw (BusinessException) e;
      }
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Không thể xác thực sản phẩm do lỗi kết nối nội bộ");
    }
  }
}
