package com.madhurgram.productservice.product.controller;

import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.admin.service.AdminProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicProductController {

    private final AdminProductService productService;

    public PublicProductController(AdminProductService productService) {
        this.productService = productService;
    }

    // 🌍 बिना किसी टोकन के सारे प्रोडक्ट्स मंगाने का रास्ता
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getAllPublicProducts() {
        return ResponseEntity.ok(productService.getAllActiveProductsForPublic()); 
    }
}