package com.madhurgram.productservice.marketing.service.impl;

import com.madhurgram.productservice.marketing.dto.BroadcastCampaignRequestDTO;
import com.madhurgram.productservice.marketing.dto.MarketingSegment;
import com.madhurgram.productservice.marketing.service.MarketingRecipientService;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service implementation for resolving client phone number lists target segments 
 * for active marketing campaigns.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class MarketingRecipientServiceImpl implements MarketingRecipientService {

    private static final String OIL_PRODUCT_KEYWORD = "oil";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * Constructor injection for MarketingRecipientServiceImpl.
     *
     * @param orderRepository   order repository dependency
     * @param productRepository product repository dependency
     */
    public MarketingRecipientServiceImpl(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Resolves target recipient phone numbers matching segment rules.
     *
     * @param request target campaign request details DTO
     * @return list of customer phone numbers
     */
    @Override
    public List<String> resolveRecipients(BroadcastCampaignRequestDTO request) {
        log.info("Resolving recipients list for target segment: '{}'", request.targetSegment());
        
        if (request.targetSegment() == null || request.targetSegment().trim().isEmpty()) {
            throw new IllegalArgumentException("Target segment is required.");
        }

        Long productId = request.productId();
        validateProductReference(productId);

        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        MarketingSegment segment = MarketingSegment.fromValue(request.targetSegment().trim().toUpperCase());

        List<String> phoneNumbers = switch (segment) {
            case TOP_SPENDERS -> orderRepository.findTopSpenderPhoneNumbersSince(threeMonthsAgo);
            case INACTIVE_CUSTOMERS -> orderRepository.findInactiveCustomerPhoneNumbersBefore(threeMonthsAgo);
            case OIL_BUYERS -> orderRepository.findCustomerPhoneNumbersByProductKeywordSince(OIL_PRODUCT_KEYWORD, threeMonthsAgo);
        };

        log.info("Resolved {} recipient phone numbers for segment: '{}'", phoneNumbers.size(), segment);
        return phoneNumbers;
    }

    private void validateProductReference(Long productId) {
        if (productId == null) {
            return;
        }

        if (productId <= 0) {
            throw new IllegalArgumentException("Product ID must be a positive number.");
        }

        if (productRepository.findById(productId).isEmpty()) {
            log.warn("Target product validation failed: Product ID: {} does not exist in database catalog", productId);
            throw new IllegalArgumentException("Product with id " + productId + " does not exist.");
        }
    }
}
