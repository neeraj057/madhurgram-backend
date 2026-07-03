package com.madhurgram.productservice.service;

import com.madhurgram.productservice.dto.ProductDTO;
import java.util.List;

public interface AdminProductService {
    List<ProductDTO> getAllProductsForAdmin();
    ProductDTO addProduct(ProductDTO productDTO);
    ProductDTO updateProduct(Long id, ProductDTO productDTO);
}