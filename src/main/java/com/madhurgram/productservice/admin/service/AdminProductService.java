package com.madhurgram.productservice.admin.service;

import com.madhurgram.productservice.product.dto.ProductDTO;
import java.util.List;

public interface AdminProductService {
    // list of all products including inactive ones
    List<ProductDTO> getAllProductsForAdmin();

    // list of all active products for public
    List<ProductDTO> getAllActiveProductsForPublic();

    // add new product
    ProductDTO addProduct(ProductDTO productDTO);

    // update product
    ProductDTO updateProduct(Long id, ProductDTO productDTO);

    // delete product
    void deleteProduct(Long id);
}