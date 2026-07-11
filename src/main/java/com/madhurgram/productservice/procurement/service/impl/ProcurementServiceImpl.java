package com.madhurgram.productservice.procurement.service.impl;

import com.madhurgram.productservice.common.service.EmailService;
import com.madhurgram.productservice.procurement.entity.PurchaseOrder;
import com.madhurgram.productservice.procurement.repository.PurchaseOrderRepository;
import com.madhurgram.productservice.procurement.service.ProcurementService;
import com.madhurgram.productservice.product.entity.Product;
import com.madhurgram.productservice.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProcurementServiceImpl implements ProcurementService {

    private static final Logger log = LoggerFactory.getLogger(ProcurementServiceImpl.class);

    private final PurchaseOrderRepository poRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;

    public ProcurementServiceImpl(
            PurchaseOrderRepository poRepository,
            ProductRepository productRepository,
            EmailService emailService
    ) {
        this.poRepository = poRepository;
        this.productRepository = productRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return poRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    @Transactional
    public PurchaseOrder draftPurchaseOrder(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        // Check if a DRAFT purchase order already exists for this product
        Optional<PurchaseOrder> existingDraft = poRepository.findByProductIdAndStatus(productId, "DRAFT");
        if (existingDraft.isPresent()) {
            log.info("Purchase Order draft already exists for product ID: {}. Skipping auto-draft.", productId);
            return existingDraft.get();
        }

        log.info("Creating a new restock Purchase Order draft for product: {}", product.getName());
        PurchaseOrder po = PurchaseOrder.builder()
                .product(product)
                .quantity(quantity != null ? quantity : 50)
                .supplierName("Gopiganj Traditional Co.")
                .supplierEmail("rawmaterials@madhurgram.com")
                .status("DRAFT")
                .build();

        return poRepository.save(po);
    }

    @Override
    @Transactional
    public PurchaseOrder updatePurchaseOrder(Long id, Integer quantity, String supplierName, String supplierEmail) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found with ID: " + id));

        if (!"DRAFT".equals(po.getStatus())) {
            throw new IllegalStateException("Only drafted Purchase Orders can be updated.");
        }

        if (quantity != null) po.setQuantity(quantity);
        if (supplierName != null) po.setSupplierName(supplierName);
        if (supplierEmail != null) po.setSupplierEmail(supplierEmail);

        log.info("Updated Purchase Order ID: {} draft details.", id);
        return poRepository.save(po);
    }

    @Override
    @Transactional
    public PurchaseOrder approvePurchaseOrder(Long id) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found with ID: " + id));

        if (!"DRAFT".equals(po.getStatus())) {
            throw new IllegalStateException("Purchase Order has already been approved/sent.");
        }

        po.setStatus("APPROVED");
        po.setApprovedAt(LocalDateTime.now());
        PurchaseOrder saved = poRepository.save(po);

        // Send Email to Supplier
        String subject = String.format("[APPROVED RESTOCK REQUEST] MadhurGram PO Ref: PO-%05d", saved.getId());
        String body = String.format(
                "Dear %s,\n\n" +
                "Please accept this approved Purchase Order for restocking our warehouse inventory:\n\n" +
                "Purchase Order Ref: PO-%05d\n" +
                "Product: %s (%s)\n" +
                "Quantity Requested: %d units\n" +
                "Destination Address: MadhurGram Warehouse, Sarafa Bazar, Indore, MP.\n\n" +
                "Kindly process this restock order and send us the dispatch details.\n\n" +
                "Regards,\n" +
                "MadhurGram Procurement Operations Team",
                saved.getSupplierName(), saved.getId(), saved.getProduct().getName(), saved.getProduct().getVolume(), saved.getQuantity()
        );

        try {
            emailService.sendEmail(saved.getSupplierEmail(), subject, body);
            log.info("Successfully approved and emailed Purchase Order ID: {} to {}", saved.getId(), saved.getSupplierEmail());
        } catch (Exception e) {
            log.error("Failed to send Purchase Order email to supplier for PO ID: {}", saved.getId(), e);
        }

        return saved;
    }
}
