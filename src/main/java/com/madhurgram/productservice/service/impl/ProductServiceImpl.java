package com.madhurgram.productservice.service.impl;

import com.madhurgram.productservice.entity.Product;
import com.madhurgram.productservice.repository.ProductRepository;
import com.madhurgram.productservice.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> getAllActiveProducts() {
        return productRepository.findByIsActiveTrue();
    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndIsActiveTrue(category.toLowerCase().trim());
    }

    @Override
    @Transactional
    public void deductProductStock(Long productId, Integer quantity) {
        int rowsUpdated = productRepository.deductStock(productId, quantity);
        if (rowsUpdated == 0) {
            throw new RuntimeException("Insufficient inventory or product not found for ID: " + productId);
        }
    }
}