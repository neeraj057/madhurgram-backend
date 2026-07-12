package com.madhurgram.productservice.product.service.impl;

import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.product.entity.Product;
import com.madhurgram.productservice.product.mapper.ProductMapper;
import com.madhurgram.productservice.product.repository.ProductRepository;
import com.madhurgram.productservice.product.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductRepository productRepository;
    private final com.madhurgram.productservice.procurement.service.ProcurementService procurementService;
    private final ProductMapper productMapper;

    public ProductServiceImpl(
            ProductRepository productRepository,
            @org.springframework.context.annotation.Lazy com.madhurgram.productservice.procurement.service.ProcurementService procurementService,
            ProductMapper productMapper
    ) {
        this.productRepository = productRepository;
        this.procurementService = procurementService;
        this.productMapper = productMapper;
    }

    @Override
    @Cacheable(value = "products", key = "'active'")
    public List<ProductDTO> getAllActiveProducts() {
        log.info("[CACHE MISS] Fetching all active products from database...");
        List<Product> products = productRepository.findByIsActiveTrue();
        return products.stream()
                .map(productMapper::toProductDTO)
                .toList();
    }

    @Override
    @Cacheable(value = "products", key = "#category.toLowerCase().trim()")
    public List<ProductDTO> getProductsByCategory(String category) {
        log.info("[CACHE MISS] Fetching products by category '{}' from database...", category);
        List<Product> products = productRepository.findByCategoryAndIsActiveTrue(category.toLowerCase().trim());
        return products.stream()
                .map(productMapper::toProductDTO)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    public void deductProductStock(Long productId, Integer quantity) {
        log.info("Attempting to deduct stock for product ID: {} (Quantity: {})", productId, quantity);
        int rowsUpdated = productRepository.deductStock(productId, quantity);
        if (rowsUpdated == 0) {
            log.error("Failed to deduct stock: Product ID: {} has insufficient inventory or does not exist", productId);
            throw new RuntimeException("Insufficient inventory or product not found for ID: " + productId);
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

    @Override
    @Transactional
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    public void restoreProductStock(Long productId, Integer quantity) {
        log.info("Attempting to restore stock for product ID: {} (Quantity: {})", productId, quantity);
        int rowsUpdated = productRepository.restoreStock(productId, quantity);
        if (rowsUpdated == 0) {
            log.error("Failed to restore stock: Product ID: {} does not exist", productId);
            throw new RuntimeException("Failed to restore inventory. Product not found for ID: " + productId);
        }
        log.info("Successfully restored stock for product ID: {} (Quantity: {}). Invalidating caches.", productId, quantity);
    }
}