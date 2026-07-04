package com.madhurgram.productservice.controller;

import com.madhurgram.productservice.entity.Product;
import com.madhurgram.productservice.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = { "http://localhost:3000", "http://192.168.31.211:3000" })
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(value = "category", required = false, defaultValue = "shop-all") String category) {

        List<Product> products;
        if ("shop-all".equalsIgnoreCase(category)) {
            products = productService.getAllActiveProducts();
        } else {
            products = productService.getProductsByCategory(category);
        }

        return ResponseEntity.ok(products);
    }

}