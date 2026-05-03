package com.ecommerce.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", url = "${PRODUCT_SERVICE_URL:http://product-service:8081}")
public interface ProductServiceClient {

    @PostMapping("/products/{id}/reserve")
    void reserveStock(@PathVariable("id") Long productId, @RequestParam("quantity") Integer quantity);
}
