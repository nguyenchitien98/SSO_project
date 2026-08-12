package com.sso.order.client;

import com.sso.common.dto.ApiResponse;
import com.sso.order.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client liên kết tới Product Service để tra cứu thông tin sản phẩm.
 *
 * @author SSO Platform Team
 * @since Sprint 14
 */
@FeignClient(name = "product-service", path = "/api/products")
public interface ProductClient {

  @GetMapping("/{id}")
  ApiResponse<ProductDto> getProductById(@PathVariable("id") Long id);
}
