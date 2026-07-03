package com.madhurgram.productservice.controller;

import com.madhurgram.productservice.dto.ProductDTO;
import com.madhurgram.productservice.service.AdminProductService; // तुम्हारा जो भी सर्विस इंटरफ़ेस है
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*") // Next.js के लिए CORS
public class PublicProductController {

    private final AdminProductService productService;

    public PublicProductController(AdminProductService productService) {
        this.productService = productService;
    }

    // 🌍 बिना किसी टोकन के सारे प्रोडक्ट्स मंगाने का रास्ता
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getAllPublicProducts() {
        // नोट: आइडियली यहाँ हमें सिर्फ वो प्रोडक्ट्स भेजने चाहिए जिनका isActive = true हो।
        // अभी के लिए हम तुम्हारी मौजूदा सर्विस यूज़ कर रहे हैं।
        return ResponseEntity.ok(productService.getAllProductsForAdmin()); 
    }
}