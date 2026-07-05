package com.madhurgram.productservice.product.service.impl;

import com.madhurgram.productservice.product.entity.Product;
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

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Cacheable(value = "products", key = "'active'")
    public List<Product> getAllActiveProducts() {
        log.info("[CACHE MISS] Fetching all active products from database...");
        return productRepository.findByIsActiveTrue();
    }

    @Override
    @Cacheable(value = "products", key = "#category.toLowerCase().trim()")
    public List<Product> getProductsByCategory(String category) {
        log.info("[CACHE MISS] Fetching products by category '{}' from database...", category);
        return productRepository.findByCategoryAndIsActiveTrue(category.toLowerCase().trim());
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