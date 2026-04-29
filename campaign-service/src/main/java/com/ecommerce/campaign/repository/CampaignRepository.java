package com.ecommerce.campaign.repository;

import com.ecommerce.campaign.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Optional<Campaign> findByCodeIgnoreCase(String code);
}
