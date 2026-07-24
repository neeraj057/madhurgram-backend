package com.madhurgram.productservice.product.service;

import com.madhurgram.productservice.product.dto.ProductDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    List<ProductDTO> getAllActiveProducts();
    Page<ProductDTO> getAllActiveProducts(Pageable pageable);
    
    List<ProductDTO> getProductsByCategory(String category);
    Page<ProductDTO> getProductsByCategory(String category, Pageable pageable);

    List<String> getAllActiveCategories();

    void deductProductStock(Long productId, Integer quantity);
    void restoreProductStock(Long productId, Integer quantity);
}