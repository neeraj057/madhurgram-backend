package com.madhurgram.productservice.marketing.service;

import com.madhurgram.productservice.marketing.dto.BroadcastCampaignDTO;
import com.madhurgram.productservice.marketing.dto.BroadcastCampaignRequestDTO;

import java.util.List;

public interface MarketingService {
    BroadcastCampaignDTO createCampaign(BroadcastCampaignRequestDTO request);
    List<BroadcastCampaignDTO> getCampaignSummaries();
}
