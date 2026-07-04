package com.madhurgram.productservice.admin.service;

import com.madhurgram.productservice.product.dto.ProductDTO;
import java.util.List;

public interface AdminProductService {
    List<ProductDTO> getAllProductsForAdmin();
    List<ProductDTO> getAllActiveProductsForPublic();
    ProductDTO addProduct(ProductDTO productDTO);
    ProductDTO updateProduct(Long id, ProductDTO productDTO);
    void deleteProduct(Long id); // 👈 प्रोडक्ट डिलीट करने का कॉन्ट्रैक्ट
}