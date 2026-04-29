package com.ecommerce.cart.client;

import com.ecommerce.cart.dto.CampaignValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "campaign-service")
public interface CampaignClient {

    @GetMapping("/campaigns/validate")
    CampaignValidationResponse validateCampaign(@RequestParam("code") String code);
}
