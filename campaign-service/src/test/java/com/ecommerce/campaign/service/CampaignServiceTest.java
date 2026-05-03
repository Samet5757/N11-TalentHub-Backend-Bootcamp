package com.ecommerce.campaign.service;

import com.ecommerce.campaign.dto.CampaignResponse;
import com.ecommerce.campaign.entity.Campaign;
import com.ecommerce.campaign.entity.DiscountType;
import com.ecommerce.campaign.exception.CampaignNotFoundException;
import com.ecommerce.campaign.repository.CampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private CampaignService campaignService;

    @Test
    void validateCampaign_whenActive_shouldReturnResponse() {
        Campaign campaign = new Campaign();
        campaign.setId(5L);
        campaign.setCode("INDIRIM10");
        campaign.setDiscountType(DiscountType.PERCENTAGE);
        campaign.setDiscountValue(10.0);
        campaign.setActive(true);

        when(campaignRepository.findByCodeIgnoreCase("INDIRIM10")).thenReturn(Optional.of(campaign));

        CampaignResponse response = campaignService.validateCampaign("INDIRIM10");

        assertEquals(5L, response.id());
        assertEquals("INDIRIM10", response.code());
        assertEquals(DiscountType.PERCENTAGE, response.discountType());
        assertEquals(10.0, response.discountValue());
        assertTrue(response.active());
    }

    @Test
    void validateCampaign_whenInactive_shouldThrow() {
        Campaign campaign = new Campaign();
        campaign.setCode("PASIF");
        campaign.setActive(false);

        when(campaignRepository.findByCodeIgnoreCase("PASIF")).thenReturn(Optional.of(campaign));

        assertThrows(CampaignNotFoundException.class, () -> campaignService.validateCampaign("PASIF"));
    }

    @Test
    void validateCampaign_whenBlankCode_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> campaignService.validateCampaign(" "));
    }

    @Test
    void validateCampaign_whenNotFound_shouldThrow() {
        when(campaignRepository.findByCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(CampaignNotFoundException.class, () -> campaignService.validateCampaign("UNKNOWN"));
    }

    @Test
    void validateCampaign_shouldTrimInputCode() {
        Campaign campaign = new Campaign();
        campaign.setId(9L);
        campaign.setCode("KOD1");
        campaign.setDiscountType(DiscountType.FLAT_AMOUNT);
        campaign.setDiscountValue(25.0);
        campaign.setActive(true);

        when(campaignRepository.findByCodeIgnoreCase("KOD1")).thenReturn(Optional.of(campaign));

        CampaignResponse response = campaignService.validateCampaign("  KOD1  ");

        assertEquals(9L, response.id());
        assertEquals("KOD1", response.code());
    }
}
