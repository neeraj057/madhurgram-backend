package com.madhurgram.productservice.admin.controller;

import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.admin.service.AdminProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@CrossOrigin(origins = "*") // CORS for Next.js
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final com.madhurgram.productservice.audit.service.AuditLogService auditLogService;

    // 🚀 Clean Constructor Injection with new Admin Service
    public AdminProductController(AdminProductService adminProductService, com.madhurgram.productservice.audit.service.AuditLogService auditLogService) {
        this.adminProductService = adminProductService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(adminProductService.getAllProductsForAdmin());
    }

    @PostMapping
    public ResponseEntity<ProductDTO> addProduct(@RequestBody ProductDTO productDTO) {
        if (productDTO.getName() == null || productDTO.getPrice() == null) {
            throw new IllegalArgumentException("Product Name and Price are mandatory.");
        }
        ProductDTO newProduct = adminProductService.addProduct(productDTO);
        auditLogService.log("ADD_PRODUCT", String.valueOf(newProduct.getId()), 
                "Created new product: " + newProduct.getName() + " with price: " + newProduct.getPrice());
        return ResponseEntity.status(HttpStatus.CREATED).body(newProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = adminProductService.updateProduct(id, productDTO);
        auditLogService.log("UPDATE_PRODUCT", String.valueOf(id), 
                "Updated product details: " + productDTO.getName() + ", Price: " + productDTO.getPrice() + ", Stock: " + productDTO.getStock());
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        adminProductService.deleteProduct(id);
        auditLogService.log("DELETE_PRODUCT", String.valueOf(id), 
                "Admin deleted product ID: " + id);
        return ResponseEntity.noContent().build();
    }
}