package com.madhurgram.productservice.product.controller;

import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.product.service.ProductService;

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
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Public catalog retrieval endpoints for shoppers")
public class ProductController {

    /** Default category query parameter fallback. */
    public static final String DEFAULT_CATEGORY = "shop-all";

    private final ProductService productService;

    /**
     * Constructor injection for ProductController.
     *
     * @param productService service for querying products
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Fetches active products filtered optionally by category.
     *
     * @param category the name of the product category (defaults to 'shop-all')
     * @return a list of matching active products
     */
    @GetMapping
    @Operation(summary = "Get products by category", description = "Fetches list of active products. Returns all active products if category is empty or 'shop-all'.")
    public ResponseEntity<List<ProductDTO>> getProducts(
            @RequestParam(value = "category", required = false, defaultValue = DEFAULT_CATEGORY) String category) {
        log.info("Request received to fetch products for category: {}", category);

        List<ProductDTO> products;
        if (DEFAULT_CATEGORY.equalsIgnoreCase(category)) {
            products = productService.getAllActiveProducts();
        } else {
            products = productService.getProductsByCategory(category);
        }

        log.info("Returning {} product(s) for category: {}", products.size(), category);
        return ResponseEntity.ok(products);
    }
}