package com.madhurgram.productservice.service;

import com.madhurgram.productservice.entity.Product;
import java.util.List;

public interface ProductService {
    List<Product> getAllActiveProducts();
    List<Product> getProductsByCategory(String category);
    void deductProductStock(Long productId, Integer quantity); // 👈 स्टॉक कम करने का कॉन्ट्रैक्ट
    
}