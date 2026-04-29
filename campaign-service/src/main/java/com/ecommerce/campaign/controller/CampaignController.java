package com.ecommerce.campaign.controller;

import com.ecommerce.campaign.dto.CampaignResponse;
import com.ecommerce.campaign.service.CampaignService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping("/validate")
    public ResponseEntity<CampaignResponse> validateCampaign(@RequestParam String code) {
        return ResponseEntity.ok(campaignService.validateCampaign(code));
    }
}
