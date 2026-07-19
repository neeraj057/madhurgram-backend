package com.madhurgram.productservice.product.controller;

import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.admin.service.AdminProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for retrieving public product data without authentication/token checks.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/public")
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
     * Retrieves all active products in a public format, optionally paginated.
     * Accessible by unauthenticated shoppers.
     *
     * @param page optional page index (0-based)
     * @param size optional page size limit
     * @return list or page of active products as DTOs
     */
    @GetMapping("/products")
    @Operation(summary = "Get all public products", description = "Fetches a simplified list of all active products for the public storefront. Supports optional pagination parameters page and size.")
    public ResponseEntity<?> getAllPublicProducts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        log.info("Public request: fetch active products (page={}, size={})", page, size);
        
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
            Page<ProductDTO> paginated = productService.getAllActiveProductsForPublic(pageable);
            log.info("Returning paginated page {} with {} public product(s)", page, paginated.getNumberOfElements());
            return ResponseEntity.ok(paginated);
        } else {
            List<ProductDTO> products = productService.getAllActiveProductsForPublic();
            log.info("Returning {} unpaginated public product(s)", products.size());
            return ResponseEntity.ok(products);
        }
    }
}