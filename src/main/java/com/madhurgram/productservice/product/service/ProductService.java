package com.madhurgram.productservice.product.service;

import com.madhurgram.productservice.product.entity.Product;
import java.util.List;

public interface ProductService {
    List<Product> getAllActiveProducts();
    List<Product> getProductsByCategory(String category);
    void deductProductStock(Long productId, Integer quantity); // 👈 स्टॉक कम करने का कॉन्ट्रैक्ट
    void restoreProductStock(Long productId, Integer quantity);
}