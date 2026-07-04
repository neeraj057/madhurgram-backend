package com.madhurgram.productservice.service.impl;

import com.madhurgram.productservice.dto.ProductDTO;
import com.madhurgram.productservice.entity.Product;
import com.madhurgram.productservice.repository.ProductRepository;
import com.madhurgram.productservice.service.AdminProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;

    public AdminProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<ProductDTO> getAllProductsForAdmin() {
        // एडमिन को सारे प्रोडक्ट्स दिखेंगे (चाहे वो Out of Stock हों या Inactive)
        return productRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductDTO addProduct(ProductDTO dto) {
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
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
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
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        productRepository.deleteById(id);
    }

    // Mapper helper
    private ProductDTO mapToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .volume(product.getVolume())
                .imageUrl(product.getImageUrl())
                .stock(product.getStock())
                .isActive(product.isActive())
                .build();
    }
}