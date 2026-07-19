package com.madhurgram.productservice.procurement.scheduler;

import com.madhurgram.productservice.procurement.entity.PurchaseOrder;
import com.madhurgram.productservice.procurement.repository.PurchaseOrderRepository;
import com.madhurgram.productservice.product.entity.Product;
import com.madhurgram.productservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchExpiryScheduler {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;

    // Runs every day at 1:00 AM
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void processNearingExpiryBatches() {
        log.info("[BatchExpiryScheduler] Starting batch expiry check for clearance sale...");
        
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(45); // 45 days threshold

        List<PurchaseOrder> expiringBatches = purchaseOrderRepository.findByExpiryDateBetween(today, thresholdDate);
        
        if (expiringBatches.isEmpty()) {
            log.info("[BatchExpiryScheduler] No batches found nearing expiry in the next 45 days.");
            return;
        }

        log.info("[BatchExpiryScheduler] Found {} batches nearing expiry.", expiringBatches.size());

        for (PurchaseOrder batch : expiringBatches) {
            Product product = batch.getProduct();
            if (product != null && !product.isClearanceActive()) {
                applyClearanceDiscount(product);
            }
        }
        
        log.info("[BatchExpiryScheduler] Completed batch expiry processing.");
    }

    private void applyClearanceDiscount(Product product) {
        log.warn("ADMIN ALERT: Product ID: {} - {} has a batch nearing expiry in < 45 days. Please review and apply clearance sale manually if desired.", product.getId(), product.getName());
        // Auto-discount logic removed per business rules (small batches).
        // Admin must manually trigger clearance if needed.
    }
}
