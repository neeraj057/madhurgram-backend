package com.madhurgram.productservice.marketing.service.impl;

import com.madhurgram.productservice.marketing.dto.BroadcastCampaignDTO;
import com.madhurgram.productservice.marketing.dto.BroadcastCampaignRequestDTO;
import com.madhurgram.productservice.marketing.dto.MarketingSegment;
import com.madhurgram.productservice.marketing.entity.BroadcastCampaign;
import com.madhurgram.productservice.marketing.mapper.MarketingMapper;
import com.madhurgram.productservice.marketing.repository.BroadcastCampaignRepository;
import com.madhurgram.productservice.marketing.service.MarketingRecipientService;
import com.madhurgram.productservice.marketing.service.MarketingService;
import com.madhurgram.productservice.marketing.service.SmsSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketingServiceImpl implements MarketingService {

    private static final Logger logger = LoggerFactory.getLogger(MarketingServiceImpl.class);

    private final BroadcastCampaignRepository campaignRepository;
    private final MarketingRecipientService recipientService;
    private final SmsSenderService smsSenderService;
    private final MarketingMapper marketingMapper;

    public MarketingServiceImpl(BroadcastCampaignRepository campaignRepository, MarketingRecipientService recipientService, SmsSenderService smsSenderService, MarketingMapper marketingMapper) {
        this.campaignRepository = campaignRepository;
        this.recipientService = recipientService;
        this.smsSenderService = smsSenderService;
        this.marketingMapper = marketingMapper;
    }

    @Override
    @Transactional
    public BroadcastCampaignDTO createCampaign(BroadcastCampaignRequestDTO request) {
        List<String> recipients = recipientService.resolveRecipients(request);
        int recipientsCount = recipients.size();
        int conversions = 0;
        BroadcastCampaign campaign = marketingMapper.toEntity(request, 0, conversions, LocalDateTime.now());

        BroadcastCampaign saved = campaignRepository.save(campaign);

        if (!recipients.isEmpty()) {
            smsSenderService.sendBroadcastMessageAsync(saved.getId(), recipients, request.message());
            logger.info("Triggered asynchronous Marketing Broadcast to {} target recipients for campaign: {}", recipientsCount, saved.getTitle());
        }

        return marketingMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BroadcastCampaignDTO> getCampaignSummaries() {
        return campaignRepository.findAll().stream()
                .map(c -> new BroadcastCampaignDTO(
                        c.getId(),
                        c.getTitle(),
                        c.getMessage(),
                        c.getTargetSegment(),
                        c.getProductKeyword(),
                        c.getProductId(),
                        c.getRecipients(),
                        c.getConversions(),
                        c.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
