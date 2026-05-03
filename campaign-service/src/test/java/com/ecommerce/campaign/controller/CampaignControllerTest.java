package com.ecommerce.campaign.controller;

import com.ecommerce.campaign.dto.CampaignResponse;
import com.ecommerce.campaign.entity.Campaign;
import com.ecommerce.campaign.entity.DiscountType;
import com.ecommerce.campaign.repository.CampaignRepository;
import com.ecommerce.campaign.service.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignControllerTest {

    @Mock
    private CampaignRepository campaignRepository;

    private CampaignController campaignController;

    @BeforeEach
    void setUp() {
        CampaignService campaignService = new CampaignService(campaignRepository);
        campaignController = new CampaignController(campaignService);
    }

    @Test
    void validateCampaign_shouldReturnResponse() {
        Campaign campaign = new Campaign();
        campaign.setId(5L);
        campaign.setCode("INDIRIM10");
        campaign.setDiscountType(DiscountType.PERCENTAGE);
        campaign.setDiscountValue(10.0);
        campaign.setActive(true);
        when(campaignRepository.findByCodeIgnoreCase("INDIRIM10")).thenReturn(Optional.of(campaign));

        ResponseEntity<CampaignResponse> response = campaignController.validateCampaign("INDIRIM10");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("INDIRIM10", response.getBody().code());
        assertEquals(10.0, response.getBody().discountValue());
    }
}
