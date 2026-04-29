package com.ecommerce.campaign.service;

import com.ecommerce.campaign.dto.CampaignResponse;
import com.ecommerce.campaign.entity.Campaign;
import com.ecommerce.campaign.exception.CampaignNotFoundException;
import com.ecommerce.campaign.repository.CampaignRepository;
import org.springframework.stereotype.Service;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    public CampaignResponse validateCampaign(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Campaign code is required");
        }

        Campaign campaign = campaignRepository.findByCodeIgnoreCase(code.trim())
                .filter(Campaign::isActive)
                .orElseThrow(() -> new CampaignNotFoundException(code));

        return new CampaignResponse(
                campaign.getId(),
                campaign.getCode(),
                campaign.getDiscountType(),
                campaign.getDiscountValue(),
                campaign.isActive()
        );
    }
}
