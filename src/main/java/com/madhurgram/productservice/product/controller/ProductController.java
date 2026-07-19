package com.madhurgram.productservice.product.controller;

import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.product.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for retrieving active public products.
 * 
 * <p>Supports listing products filtered by category with a default behavior
 * of showing all items under the "shop-all" category.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Public catalog retrieval endpoints for shoppers")
public class ProductController {

    public static final String DEFAULT_CATEGORY = "shop-all";

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Get products by category", description = "Fetches list of active products. Returns all active products if category is empty or 'shop-all'. Supports optional pagination parameters page and size.")
    public ResponseEntity<?> getProducts(
            @RequestParam(value = "category", required = false, defaultValue = DEFAULT_CATEGORY) String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        log.info("Request received to fetch products for category: {} (page={}, size={})", category, page, size);

        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
            Page<ProductDTO> paginated = productService.getProductsByCategory(category, pageable);
            log.info("Returning paginated page {} with {} product(s) for category: {}", page, paginated.getNumberOfElements(), category);
            return ResponseEntity.ok(paginated);
        } else {
            List<ProductDTO> products = productService.getProductsByCategory(category);
            log.info("Returning {} unpaginated product(s) for category: {}", products.size(), category);
            return ResponseEntity.ok(products);
        }
    }
}