package com.ecommerce.payment.client;

import com.ecommerce.payment.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "order-service",
        url = "${ORDER_SERVICE_URL:http://order-service:8083}",
        configuration = FeignConfig.class
)
public interface OrderClient {

    @PutMapping("/orders/{id}/status")
    void updateOrderStatus(@PathVariable("id") Long orderId, @RequestParam("status") String status);
}
