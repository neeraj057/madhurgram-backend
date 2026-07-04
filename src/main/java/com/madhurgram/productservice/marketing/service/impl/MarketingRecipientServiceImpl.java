package com.madhurgram.productservice.marketing.service.impl;

import com.madhurgram.productservice.marketing.dto.BroadcastCampaignRequestDTO;
import com.madhurgram.productservice.marketing.dto.MarketingSegment;
import com.madhurgram.productservice.marketing.service.MarketingRecipientService;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MarketingRecipientServiceImpl implements MarketingRecipientService {

    private static final String OIL_PRODUCT_KEYWORD = "oil";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public MarketingRecipientServiceImpl(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<String> resolveRecipients(BroadcastCampaignRequestDTO request) {
        Long productId = request.productId();
        validateProductReference(productId);

        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        MarketingSegment segment = MarketingSegment.fromValue(request.targetSegment());

        return switch (segment) {
            case TOP_SPENDERS -> orderRepository.findTopSpenderPhoneNumbersSince(threeMonthsAgo);
            case INACTIVE_CUSTOMERS -> orderRepository.findInactiveCustomerPhoneNumbersBefore(threeMonthsAgo);
            case OIL_BUYERS -> orderRepository.findCustomerPhoneNumbersByProductKeywordSince(OIL_PRODUCT_KEYWORD, threeMonthsAgo);
        };
    }

    private void validateProductReference(Long productId) {
        if (productId == null) {
            return;
        }

        if (productId <= 0) {
            throw new IllegalArgumentException("Product ID must be a positive number.");
        }

        if (productRepository.findById(productId).isEmpty()) {
            throw new IllegalArgumentException("Product with id " + productId + " does not exist.");
        }
    }
}
