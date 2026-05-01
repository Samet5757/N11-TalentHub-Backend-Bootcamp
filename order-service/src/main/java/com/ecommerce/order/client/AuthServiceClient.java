package com.ecommerce.order.client;

import com.ecommerce.order.client.dto.UserContactDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${AUTH_SERVICE_URL:http://auth-service:8085}")
public interface AuthServiceClient {

    @GetMapping("/auth/internal/users/{id}/contact")
    UserContactDto getUserContact(@PathVariable("id") Long id);
}
