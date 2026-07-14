package com.madhurgram.productservice.admin.service.impl;

import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.product.entity.Product;
import com.madhurgram.productservice.product.entity.HsnTaxMaster;
import com.madhurgram.productservice.product.mapper.ProductMapper;
import com.madhurgram.productservice.product.repository.ProductRepository;
import com.madhurgram.productservice.product.repository.HsnTaxMasterRepository;
import com.madhurgram.productservice.admin.service.AdminProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for administrative product catalog updates, additions, 
 * deletions, and inventory audits.
 */
@Slf4j
@Service
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final HsnTaxMasterRepository hsnTaxMasterRepository;
    private final ProductMapper productMapper;

    /**
     * Constructor injection for AdminProductServiceImpl.
     *
     * @param productRepository      product database repository
     * @param hsnTaxMasterRepository HSN tax database repository
     * @param productMapper          product mapper instance
     */
    public AdminProductServiceImpl(ProductRepository productRepository, 
                                   HsnTaxMasterRepository hsnTaxMasterRepository,
                                   ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.hsnTaxMasterRepository = hsnTaxMasterRepository;
        this.productMapper = productMapper;
    }

    /**
     * Lists all products including inactive ones for administrative monitoring.
     *
     * @return list of products as DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProductsForAdmin() {
        log.info("Admin request: fetch all products including inactive ones");
        return productRepository.findAll().stream()
                .map(productMapper::toProductDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lists active products for storefront caching.
     *
     * @return list of active products as DTOs
     */
    @Override
    @Cacheable(value = "products", key = "'public_active'")
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllActiveProductsForPublic() {
        log.info("[CACHE MISS] Fetching all active products for public catalog...");
        return productRepository.findByIsActiveTrue().stream()
                .map(productMapper::toProductDTO)
                .collect(Collectors.toList());
    }

    /**
     * Adds a new product to the catalog.
     * Evicts active catalog caches.
     *
     * @param dto product details DTO
     * @return created product details DTO
     */
    @Override
    @Transactional
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    public ProductDTO addProduct(ProductDTO dto) {
        log.info("Adding new product: {}. Invalidating caches.", dto.getName());
        
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty.");
        }
        
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Product category cannot be null or empty.");
        }

        HsnTaxMaster hsn = null;
        if (dto.getHsnCode() != null && !dto.getHsnCode().trim().isEmpty()) {
            hsn = hsnTaxMasterRepository.findById(dto.getHsnCode().trim()).orElse(null);
        }

        Product product = Product.builder()
                .name(dto.getName().trim())
                .price(dto.getPrice())
                .originalPrice(dto.getOriginalPrice())
                .volume(dto.getVolume())
                .imageUrl(dto.getImageUrl())
                .stock(dto.getStock())
                .category(dto.getCategory().trim())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .tag(dto.getTag())
                .rating(dto.getRating() != null ? dto.getRating() : java.math.BigDecimal.valueOf(4.8))
                .showSalesCount(dto.getShowSalesCount() != null ? dto.getShowSalesCount() : false)
                .salesCount(dto.getSalesCount() != null ? dto.getSalesCount() : 0)
                .hsnTaxMaster(hsn)
                .build();
        
        Product savedProduct = productRepository.save(product);
        log.info("Successfully added new product with ID: {}", savedProduct.getId());
        return productMapper.toProductDTO(savedProduct);
    }

    /**
     * Updates details of an existing product.
     * Evicts catalog and analytic caches.
     *
     * @param id  target product ID
     * @param dto updated details
     * @return updated product details DTO
     */
    @Override
    @Transactional
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        log.info("Updating product ID: {}. Invalidating caches.", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Product ID cannot be null.");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));

        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Product category cannot be null or empty.");
        }

        HsnTaxMaster hsn = null;
        if (dto.getHsnCode() != null && !dto.getHsnCode().trim().isEmpty()) {
            hsn = hsnTaxMasterRepository.findById(dto.getHsnCode().trim()).orElse(null);
        }

        product.setName(dto.getName() != null ? dto.getName().trim() : product.getName());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setVolume(dto.getVolume());
        product.setImageUrl(dto.getImageUrl());
        product.setStock(dto.getStock());
        product.setCategory(dto.getCategory().trim());
        product.setHsnTaxMaster(hsn);
        product.setTag(dto.getTag());
        if (dto.getRating() != null) {
            product.setRating(dto.getRating());
        }
        if (dto.getShowSalesCount() != null) {
            product.setShowSalesCount(dto.getShowSalesCount());
        }
        if (dto.getSalesCount() != null) {
            product.setSalesCount(dto.getSalesCount());
        }
        
        if (dto.getIsActive() != null) {
            product.setActive(dto.getIsActive());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Successfully updated product ID: {}", updatedProduct.getId());
        return productMapper.toProductDTO(updatedProduct);
    }

    /**
     * Deletes a product from the catalog by ID.
     * Evicts catalog and analytic caches.
     *
     * @param id target product ID
     */
    @Override
    @Transactional
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    public void deleteProduct(Long id) {
        log.info("Deleting product ID: {}. Invalidating caches.", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Product ID cannot be null.");
        }
        
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found with ID: " + id);
        }
        productRepository.deleteById(id);
        log.info("Successfully deleted product ID: {}", id);
    }
}