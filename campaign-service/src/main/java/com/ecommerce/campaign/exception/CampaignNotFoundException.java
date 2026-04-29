package com.ecommerce.campaign.exception;

public class CampaignNotFoundException extends RuntimeException {

    public CampaignNotFoundException(String code) {
        super("Campaign not found or inactive for code: " + code);
    }
}
