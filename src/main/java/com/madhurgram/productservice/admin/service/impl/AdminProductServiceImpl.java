package com.madhurgram.productservice.admin.service.impl;

import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.product.entity.Product;
import com.madhurgram.productservice.product.repository.ProductRepository;
import com.madhurgram.productservice.admin.service.AdminProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminProductServiceImpl implements AdminProductService {

    private static final Logger log = LoggerFactory.getLogger(AdminProductServiceImpl.class);
    private final ProductRepository productRepository;

    public AdminProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<ProductDTO> getAllProductsForAdmin() {
        // Direct DB read for admin console (bypassing cache to ensure live inventory sync)
        return productRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "products", key = "'public_active'")
    public List<ProductDTO> getAllActiveProductsForPublic() {
        log.info("[CACHE MISS] Fetching all active products for public catalog...");
        return productRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    public ProductDTO addProduct(ProductDTO dto) {
        log.info("Adding new product: {}. Invalidating caches.", dto.getName());
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Product category cannot be null or empty.");
        }
        Product product = Product.builder()
                .name(dto.getName())
                .price(dto.getPrice())
                .volume(dto.getVolume())
                .imageUrl(dto.getImageUrl())
                .stock(dto.getStock())
                .category(dto.getCategory().trim())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        
        Product savedProduct = productRepository.save(product);
        return mapToDTO(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        log.info("Updating product ID: {}. Invalidating caches.", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));

        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Product category cannot be null or empty.");
        }

        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setVolume(dto.getVolume());
        product.setImageUrl(dto.getImageUrl());
        product.setStock(dto.getStock());
        product.setCategory(dto.getCategory().trim());
        
        if (dto.getIsActive() != null) {
            product.setActive(dto.getIsActive());
        }

        Product updatedProduct = productRepository.save(product);
        return mapToDTO(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    public void deleteProduct(Long id) {
        log.info("Deleting product ID: {}. Invalidating caches.", id);
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductDTO mapToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .volume(product.getVolume())
                .imageUrl(product.getImageUrl())
                .stock(product.getStock())
                .isActive(product.isActive())
                .category(product.getCategory())
                .build();
    }
}