package com.madhurgram.productservice.product.service;

import com.madhurgram.productservice.product.dto.ProductDTO;
import java.util.List;

public interface ProductService {
    List<ProductDTO> getAllActiveProducts();
    List<ProductDTO> getProductsByCategory(String category);
    void deductProductStock(Long productId, Integer quantity);
    void restoreProductStock(Long productId, Integer quantity);
}