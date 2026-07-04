package com.madhurgram.productservice.service.impl;

import com.madhurgram.productservice.dto.BroadcastCampaignDTO;
import com.madhurgram.productservice.dto.BroadcastCampaignRequestDTO;
import com.madhurgram.productservice.entity.BroadcastCampaign;
import com.madhurgram.productservice.repository.BroadcastCampaignRepository;
import com.madhurgram.productservice.repository.OrderRepository;
import com.madhurgram.productservice.repository.ProductRepository;
import com.madhurgram.productservice.service.MarketingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketingServiceImpl implements MarketingService {

    private final BroadcastCampaignRepository campaignRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public MarketingServiceImpl(BroadcastCampaignRepository campaignRepository, OrderRepository orderRepository, ProductRepository productRepository) {
        this.campaignRepository = campaignRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public BroadcastCampaignDTO createCampaign(BroadcastCampaignRequestDTO request) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<String> recipients = switch (request.targetSegment().toLowerCase()) {
            case "top spenders" -> orderRepository.findTopSpenderPhoneNumbersSince(threeMonthsAgo);
            case "inactive customers" -> orderRepository.findInactiveCustomerPhoneNumbersBefore(threeMonthsAgo);
            case "oil buyers" -> orderRepository.findCustomerPhoneNumbersByProductKeywordSince("oil", threeMonthsAgo);
            default -> List.of();
        };

        int recipientsCount = recipients.size();
        int conversions = 0;

        Long productId = request.productId();
        if (productId != null) {
            productRepository.findById(productId); // just ensure product exists
        }

        BroadcastCampaign campaign = new BroadcastCampaign(
                request.title(),
                request.message(),
                request.targetSegment(),
                request.productKeyword(),
                productId,
                recipientsCount,
                conversions,
                LocalDateTime.now()
        );

        BroadcastCampaign saved = campaignRepository.save(campaign);

        // Mock send logic: for now, we only persist the campaign and log recipients.
        if (!recipients.isEmpty()) {
            System.out.println("[Marketing Broadcast] Sending message to " + recipientsCount + " customers for campaign: " + saved.getTitle());
        }

        return new BroadcastCampaignDTO(
                saved.getId(),
                saved.getTitle(),
                saved.getMessage(),
                saved.getTargetSegment(),
                saved.getProductKeyword(),
                saved.getProductId(),
                saved.getRecipients(),
                saved.getConversions(),
                saved.getCreatedAt()
        );
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
