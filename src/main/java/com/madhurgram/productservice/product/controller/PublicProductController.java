package com.madhurgram.productservice.product.controller;

import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.admin.service.AdminProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for retrieving public product data without authentication/token checks.
 */
@Slf4j
@RestController
@RequestMapping("/api/public")
@Tag(name = "Public Catalog", description = "Endpoints for fetching public product listings")
public class PublicProductController {

    private final AdminProductService productService;

    /**
     * Constructor injection for PublicProductController.
     *
     * @param productService the admin product service supplying lists
     */
    public PublicProductController(AdminProductService productService) {
        this.productService = productService;
    }

    /**
     * Retrieves all active products in a public format.
     * Accessible by unauthenticated shoppers.
     *
     * @return list of active products as DTOs
     */
    @GetMapping("/products")
    @Operation(summary = "Get all public products", description = "Fetches a simplified list of all active products for the public storefront.")
    public ResponseEntity<List<ProductDTO>> getAllPublicProducts() {
        log.info("Public request: fetch all active products");
        List<ProductDTO> products = productService.getAllActiveProductsForPublic();
        log.info("Returning {} public product(s)", products.size());
        return ResponseEntity.ok(products);
    }
}