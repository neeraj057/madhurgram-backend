package com.madhurgram.productservice.service;

import com.madhurgram.productservice.dto.BroadcastCampaignDTO;
import com.madhurgram.productservice.dto.BroadcastCampaignRequestDTO;

import java.util.List;

public interface MarketingService {
    BroadcastCampaignDTO createCampaign(BroadcastCampaignRequestDTO request);
    List<BroadcastCampaignDTO> getCampaignSummaries();
}
