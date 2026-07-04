package com.madhurgram.productservice.marketing.repository;

import com.madhurgram.productservice.marketing.entity.BroadcastCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BroadcastCampaignRepository extends JpaRepository<BroadcastCampaign, Long> {
}
