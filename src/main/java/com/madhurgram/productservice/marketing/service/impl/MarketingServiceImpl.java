package com.madhurgram.productservice.marketing.service.impl;

import com.madhurgram.productservice.marketing.dto.BroadcastCampaignDTO;
import com.madhurgram.productservice.marketing.dto.BroadcastCampaignRequestDTO;
import com.madhurgram.productservice.marketing.entity.BroadcastCampaign;
import com.madhurgram.productservice.marketing.mapper.MarketingMapper;
import com.madhurgram.productservice.marketing.repository.BroadcastCampaignRepository;
import com.madhurgram.productservice.marketing.service.MarketingRecipientService;
import com.madhurgram.productservice.marketing.service.MarketingService;
import com.madhurgram.productservice.marketing.service.SmsSenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for administering broadcast marketing campaigns and 
 * outbound recipient query dispatches.
 */
@Slf4j
@Service
public class MarketingServiceImpl implements MarketingService {

    private final BroadcastCampaignRepository campaignRepository;
    private final MarketingRecipientService recipientService;
    private final SmsSenderService smsSenderService;
    private final MarketingMapper marketingMapper;

    /**
     * Constructor injection for MarketingServiceImpl.
     *
     * @param campaignRepository marketing campaigns repository
     * @param recipientService   recipients resolver service
     * @param smsSenderService   outbound SMS provider client
     * @param marketingMapper    campaigns data mapper
     */
    public MarketingServiceImpl(
            BroadcastCampaignRepository campaignRepository, 
            MarketingRecipientService recipientService, 
            SmsSenderService smsSenderService, 
            MarketingMapper marketingMapper) {
        this.campaignRepository = campaignRepository;
        this.recipientService = recipientService;
        this.smsSenderService = smsSenderService;
        this.marketingMapper = marketingMapper;
    }

    /**
     * Creates and triggers a new outbound SMS marketing campaign broadcast.
     *
     * @param request campaign details payload DTO
     * @return created campaign summary DTO
     */
    @Override
    @Transactional
    public BroadcastCampaignDTO createCampaign(BroadcastCampaignRequestDTO request) {
        log.info("Creating new broadcast campaign: '{}'", request.title());
        
        if (request.title() == null || request.title().trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign title must not be blank.");
        }
        if (request.message() == null || request.message().trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign message must not be blank.");
        }

        List<String> recipients = recipientService.resolveRecipients(request);
        int recipientsCount = recipients.size();
        int conversions = 0;
        
        BroadcastCampaign campaign = marketingMapper.toEntity(request, recipientsCount, conversions, LocalDateTime.now());
        BroadcastCampaign saved = campaignRepository.save(campaign);

        if (!recipients.isEmpty()) {
            smsSenderService.sendBroadcastMessageAsync(saved.getId(), recipients, request.message());
            log.info("Successfully triggered async marketing broadcast to {} target recipients for campaign: '{}'", 
                    recipientsCount, saved.getTitle());
        } else {
            log.warn("Campaign '{}' has no matching target recipients. Skipping async dispatch.", saved.getTitle());
        }

        return marketingMapper.toDto(saved);
    }

    /**
     * Lists summaries of all dispatched marketing campaigns.
     *
     * @return list of campaigns DTO
     */
    @Override
    @Transactional(readOnly = true)
    public List<BroadcastCampaignDTO> getCampaignSummaries() {
        log.info("Admin request: fetch all broadcast campaign summaries");
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
