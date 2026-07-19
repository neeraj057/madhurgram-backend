package com.madhurgram.productservice.product.service.impl;

import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.product.entity.Product;
import com.madhurgram.productservice.product.mapper.ProductMapper;
import com.madhurgram.productservice.product.repository.ProductRepository;
import com.madhurgram.productservice.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Service implementation for fetching active public products and managing inventory stocks.
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final com.madhurgram.productservice.procurement.service.ProcurementService procurementService;
    private final ProductMapper productMapper;

    /**
     * Constructor injection for ProductServiceImpl.
     *
     * @param productRepository  product database access
     * @param procurementService inventory restocking service
     * @param productMapper      product catalog mapper instance
     */
    public ProductServiceImpl(
            ProductRepository productRepository,
            @org.springframework.context.annotation.Lazy com.madhurgram.productservice.procurement.service.ProcurementService procurementService,
            ProductMapper productMapper
    ) {
        this.productRepository = productRepository;
        this.procurementService = procurementService;
        this.productMapper = productMapper;
    }

    /**
     * Resolves all active products in the public catalog.
     * Caches result to speed up subsequent queries.
     *
     * @return a list of active public products as DTOs
     */
    @Override
    @Cacheable(value = "products", key = "'active'")
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllActiveProducts() {
        log.info("[CACHE MISS] Fetching all active products from database...");
        List<Product> products = productRepository.findByIsActiveTrue();
        return products.stream()
                .map(productMapper::toProductDTO)
                .toList();
    }

    @Override
    @Cacheable(value = "products", key = "'active_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllActiveProducts(Pageable pageable) {
        log.info("[CACHE MISS] Fetching paginated active products: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Product> page = productRepository.findByIsActiveTrue(pageable);
        return page.map(productMapper::toProductDTO);
    }

    /**
     * Resolves active products filtered by their category name.
     * Caches search results to speed up queries.
     *
     * @param category category code value
     * @return list of matching public products as DTOs
     */
    @Override
    @Cacheable(value = "products", key = "#category.toLowerCase().trim()")
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        log.info("[CACHE MISS] Fetching products by category '{}' from database...", category);
        
        if (category == null || category.trim().isEmpty()) {
            log.warn("Category query skipped: category parameter is empty");
            throw new IllegalArgumentException("Product category cannot be empty.");
        }

        List<Product> products = productRepository.findByCategoryAndIsActiveTrue(category.toLowerCase().trim());
        return products.stream()
                .map(productMapper::toProductDTO)
                .toList();
    }

    @Override
    @Cacheable(value = "products", key = "#category.toLowerCase().trim() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByCategory(String category, Pageable pageable) {
        log.info("[CACHE MISS] Fetching paginated products by category '{}': page={}, size={}", category, pageable.getPageNumber(), pageable.getPageSize());
        
        if (category == null || category.trim().isEmpty()) {
            log.warn("Category query skipped: category parameter is empty");
            throw new IllegalArgumentException("Product category cannot be empty.");
        }

        Page<Product> page = productRepository.findByCategoryAndIsActiveTrue(category.toLowerCase().trim(), pageable);
        return page.map(productMapper::toProductDTO);
    }

    /**
     * Deducts inventory stock quantity upon checkout.
     * Triggers auto restock purchase order draft if remaining stock drops below threshold.
     *
     * @param productId target product identifier
     * @param quantity  deduction quantity count
     */
    @Override
    @Transactional
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    public void deductProductStock(Long productId, Integer quantity) {
        log.info("Attempting to deduct stock for product ID: {} (Quantity: {})", productId, quantity);
        
        if (productId == null || quantity == null || quantity <= 0) {
            log.warn("Stock deduction aborted: product ID or quantity parameter is invalid");
            throw new IllegalArgumentException("Product ID and deduction quantity must be positive values.");
        }

        int rowsUpdated = productRepository.deductStock(productId, quantity);
        if (rowsUpdated == 0) {
            log.error("Failed to deduct stock: Product ID: {} has insufficient inventory or does not exist", productId);
            throw new IllegalArgumentException("Insufficient inventory or product not found for ID: " + productId);
        }
        log.info("Successfully deducted stock for product ID: {} (Quantity: {}). Invalidating caches.", productId, quantity);

        // Check if stock is low (<= 5)
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null && product.getStock() <= 5) {
            log.warn("Low stock detected for product: {} (Stock: {}). Auto-drafting Purchase Order.", product.getName(), product.getStock());
            try {
                procurementService.draftPurchaseOrder(productId, 50);
            } catch (Exception e) {
                log.error("Error auto-drafting Purchase Order for product ID: {}", productId, e);
            }
        }
    }

    /**
     * Restores stock to catalog due to order cancellations or item returns.
     *
     * @param productId target product identifier
     * @param quantity  quantity to restore
     */
    @Override
    @Transactional
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    public void restoreProductStock(Long productId, Integer quantity) {
        log.info("Attempting to restore stock for product ID: {} (Quantity: {})", productId, quantity);
        
        if (productId == null || quantity == null || quantity <= 0) {
            log.warn("Stock restore aborted: product ID or quantity parameter is invalid");
            throw new IllegalArgumentException("Product ID and quantity must be positive values.");
        }

        int rowsUpdated = productRepository.restoreStock(productId, quantity);
        if (rowsUpdated == 0) {
            log.error("Failed to restore stock: Product ID: {} does not exist", productId);
            throw new IllegalArgumentException("Failed to restore inventory. Product not found for ID: " + productId);
        }
        log.info("Successfully restored stock for product ID: {} (Quantity: {}). Invalidating caches.", productId, quantity);
    }
}