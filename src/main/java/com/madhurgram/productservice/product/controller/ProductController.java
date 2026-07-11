package com.madhurgram.productservice.product.controller;

import com.madhurgram.productservice.product.entity.Product;
import com.madhurgram.productservice.product.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    public static final String DEFAULT_CATEGORY = "shop-all";

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(value = "category", required = false, defaultValue = DEFAULT_CATEGORY) String category) {
        log.info("Received request to fetch products for category: {}", category);

        List<Product> products;
        if (DEFAULT_CATEGORY.equalsIgnoreCase(category)) {
            products = productService.getAllActiveProducts();
        } else {
            products = productService.getProductsByCategory(category);
        }

        log.info("Successfully fetched {} products for category: {}", products.size(), category);
        return ResponseEntity.ok(products);
    }
}