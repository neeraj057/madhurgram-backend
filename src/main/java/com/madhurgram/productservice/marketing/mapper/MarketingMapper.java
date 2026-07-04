package com.madhurgram.productservice.marketing.mapper;

import com.madhurgram.productservice.marketing.dto.BroadcastCampaignDTO;
import com.madhurgram.productservice.marketing.dto.BroadcastCampaignRequestDTO;
import com.madhurgram.productservice.marketing.entity.BroadcastCampaign;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MarketingMapper {

    public BroadcastCampaign toEntity(BroadcastCampaignRequestDTO request, int recipients, int conversions, LocalDateTime createdAt) {
        return new BroadcastCampaign(
                request.title(),
                request.message(),
                request.targetSegment(),
                request.productKeyword(),
                request.productId(),
                recipients,
                conversions,
                createdAt
        );
    }

    public BroadcastCampaignDTO toDto(BroadcastCampaign campaign) {
        return new BroadcastCampaignDTO(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getMessage(),
                campaign.getTargetSegment(),
                campaign.getProductKeyword(),
                campaign.getProductId(),
                campaign.getRecipients(),
                campaign.getConversions(),
                campaign.getCreatedAt()
        );
    }
}
