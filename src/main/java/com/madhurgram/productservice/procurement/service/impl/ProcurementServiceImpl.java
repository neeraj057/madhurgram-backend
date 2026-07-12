package com.madhurgram.productservice.procurement.service.impl;

import com.madhurgram.productservice.common.service.EmailService;
import com.madhurgram.productservice.procurement.dto.PurchaseOrderDTO;
import com.madhurgram.productservice.procurement.entity.PurchaseOrder;
import com.madhurgram.productservice.procurement.mapper.ProcurementMapper;
import com.madhurgram.productservice.procurement.repository.PurchaseOrderRepository;
import com.madhurgram.productservice.procurement.service.ProcurementService;
import com.madhurgram.productservice.product.entity.Product;
import com.madhurgram.productservice.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for managing inventory restocking purchase orders and supplier dispatch update emails.
 */
@Slf4j
@Service
public class ProcurementServiceImpl implements ProcurementService {

    private final PurchaseOrderRepository poRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final ProcurementMapper procurementMapper;

    /**
     * Constructor injection for ProcurementServiceImpl.
     *
     * @param poRepository      purchase order database repository
     * @param productRepository product catalog database repository
     * @param emailService      email client notification dispatch service
     * @param procurementMapper procurement data mapper
     */
    public ProcurementServiceImpl(
            PurchaseOrderRepository poRepository,
            ProductRepository productRepository,
            EmailService emailService,
            ProcurementMapper procurementMapper
    ) {
        this.poRepository = poRepository;
        this.productRepository = productRepository;
        this.emailService = emailService;
        this.procurementMapper = procurementMapper;
    }

    /**
     * Retrieves all purchase orders sorted chronologically descending.
     *
     * @return a list of purchase orders DTO
     */
    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderDTO> getAllPurchaseOrders() {
        log.info("Admin request: fetch all purchase orders");
        List<PurchaseOrder> pos = poRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return pos.stream()
                .map(procurementMapper::toDTO)
                .toList();
    }

    /**
     * Drafts a new purchase order for a product when inventory count hits threshold.
     * Skips drafting if a DRAFT purchase order is already outstanding.
     *
     * @param productId target product to restock
     * @param quantity  quantity to request
     * @return created purchase order DTO
     */
    @Override
    @Transactional
    public PurchaseOrderDTO draftPurchaseOrder(Long productId, Integer quantity) {
        log.info("Attempting to draft Purchase Order for product ID: {} (Quantity: {})", productId, quantity);
        
        if (productId == null) {
            throw new IllegalArgumentException("Product ID must not be null.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        Optional<PurchaseOrder> existingDraft = poRepository.findByProductIdAndStatus(productId, "DRAFT");
        if (existingDraft.isPresent()) {
            log.info("Purchase Order draft already exists for product ID: {}. Skipping auto-draft.", productId);
            return procurementMapper.toDTO(existingDraft.get());
        }

        log.info("Creating a new restock Purchase Order draft for product: {}", product.getName());
        PurchaseOrder po = PurchaseOrder.builder()
                .product(product)
                .quantity(quantity != null && quantity > 0 ? quantity : 50)
                .supplierName("Gopiganj Traditional Co.")
                .supplierEmail("rawmaterials@madhurgram.com")
                .status("DRAFT")
                .build();

        PurchaseOrder saved = poRepository.save(po);
        log.info("Successfully created restock draft PO ID: {}", saved.getId());
        return procurementMapper.toDTO(saved);
    }

    /**
     * Modifies outstanding supplier details or quantity count of a drafted purchase order.
     *
     * @param id            target purchase order ID
     * @param quantity      updated restock count limit
     * @param supplierName  updated supplier name
     * @param supplierEmail updated supplier email address
     * @return modified purchase order DTO
     */
    @Override
    @Transactional
    public PurchaseOrderDTO updatePurchaseOrder(Long id, Integer quantity, String supplierName, String supplierEmail) {
        log.info("Updating Purchase Order ID: {} details", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Purchase Order ID must not be null.");
        }

        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found with ID: " + id));

        if (!"DRAFT".equals(po.getStatus())) {
            log.warn("Update purchase order rejected: PO ID: {} is not in DRAFT state (Status: {})", id, po.getStatus());
            throw new IllegalStateException("Only drafted Purchase Orders can be updated.");
        }

        if (quantity != null && quantity <= 0) {
            throw new IllegalArgumentException("Purchase order quantity must be greater than zero.");
        }

        if (quantity != null) po.setQuantity(quantity);
        if (supplierName != null) po.setSupplierName(supplierName.trim());
        if (supplierEmail != null) po.setSupplierEmail(supplierEmail.trim());

        log.info("Updated Purchase Order ID: {} draft details successfully.", id);
        PurchaseOrder saved = poRepository.save(po);
        return procurementMapper.toDTO(saved);
    }

    /**
     * Approves and executes a purchase order draft.
     * Dispatches notification email payload to supplier address.
     *
     * @param id target purchase order ID
     * @return approved purchase order details DTO
     */
    @Override
    @Transactional
    public PurchaseOrderDTO approvePurchaseOrder(Long id) {
        log.info("Approving and dispatching Purchase Order ID: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Purchase Order ID must not be null.");
        }

        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found with ID: " + id));

        if (!"DRAFT".equals(po.getStatus())) {
            log.warn("Approve purchase order rejected: PO ID: {} is already approved/sent", id);
            throw new IllegalStateException("Purchase Order has already been approved/sent.");
        }

        po.setStatus("APPROVED");
        po.setApprovedAt(LocalDateTime.now());
        PurchaseOrder saved = poRepository.save(po);

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

        return procurementMapper.toDTO(saved);
    }
}
