package com.madhurgram.productservice.marketing.service;

import com.madhurgram.productservice.marketing.dto.BroadcastCampaignRequestDTO;

import java.util.List;

public interface MarketingRecipientService {

    List<String> resolveRecipients(BroadcastCampaignRequestDTO request);
}
