package com.madhurgram.productservice.admin.controller;

import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.admin.service.AdminProductService;
import com.madhurgram.productservice.audit.service.AuditLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing products in the admin panel.
 * 
 * <p>
 * Provides endpoints to retrieve, add, update, and delete products,
 * with built-in audit logging and logging support.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "Admin — Products", description = "Product catalog management endpoints for administrators")
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final AuditLogService auditLogService;

    /**
     * Constructor injection for required services.
     *
     * @param adminProductService the service managing product catalog operations
     * @param auditLogService     the service responsible for logging admin actions
     */
    public AdminProductController(AdminProductService adminProductService, AuditLogService auditLogService) {
        this.adminProductService = adminProductService;
        this.auditLogService = auditLogService;
    }

    /**
     * Retrieves all products in the catalog including inactive ones, optionally paginated.
     *
     * @param page optional page index (0-based)
     * @param size optional page size limit
     * @return a list or page of all products
     */
    @GetMapping
    @Operation(summary = "List all products", description = "Retrieves a list of all products including inactive ones for the admin catalog view. Supports optional pagination parameters page and size.")
    public ResponseEntity<?> getAllProducts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        log.info("Admin request: list products (page={}, size={})", page, size);
        
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
            Page<ProductDTO> paginated = adminProductService.getAllProductsForAdmin(pageable);
            log.info("Returning paginated page {} with {} product(s) to admin", page, paginated.getNumberOfElements());
            return ResponseEntity.ok(paginated);
        } else {
            List<ProductDTO> products = adminProductService.getAllProductsForAdmin();
            log.info("Returning {} unpaginated product(s) to admin", products.size());
            return ResponseEntity.ok(products);
        }
    }

    /**
     * Adds a new product to the catalog.
     * Logs an audit record upon successful creation.
     *
     * @param productDTO the product payload to add
     * @return the created product with HTTP status 201 (Created)
     * @throws IllegalArgumentException if mandatory fields (name or price) are
     *                                  missing
     */
    @PostMapping
    @Operation(summary = "Add a new product", description = "Creates a new product in the system and logs an admin audit event.")
    public ResponseEntity<ProductDTO> addProduct(@RequestBody ProductDTO productDTO) {
        log.info("Admin request: add new product '{}'", productDTO.getName());

        if (productDTO.getName() == null || productDTO.getPrice() == null) {
            log.warn("Create product failed: Name and Price are mandatory fields");
            throw new IllegalArgumentException("Product Name and Price are mandatory.");
        }

        ProductDTO newProduct = adminProductService.addProduct(productDTO);

        auditLogService.log("ADD_PRODUCT", String.valueOf(newProduct.getId()),
                "Created new product: " + newProduct.getName() + " with price: " + newProduct.getPrice());

        log.info("Product successfully created with ID: {}", newProduct.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(newProduct);
    }

    /**
     * Updates an existing product's details.
     * Logs an audit record upon successful modification.
     *
     * @param id         the ID of the product to update
     * @param productDTO the updated product details
     * @return the updated product
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product", description = "Updates product details by ID and logs an admin audit event.")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        log.info("Admin request: update product ID: {}", id);

        ProductDTO updatedProduct = adminProductService.updateProduct(id, productDTO);

        auditLogService.log("UPDATE_PRODUCT", String.valueOf(id),
                "Updated product details: " + productDTO.getName() + ", Price: " + productDTO.getPrice() + ", Stock: "
                        + productDTO.getStock());

        log.info("Product ID: {} successfully updated", id);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * Deletes (or deactivates) a product from the catalog.
     * Logs an audit record upon successful deletion.
     *
     * @param id the ID of the product to delete
     * @return HTTP status 204 (No Content)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product", description = "Deletes a product by ID from the catalog and logs an admin audit event.")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Admin request: delete product ID: {}", id);

        adminProductService.deleteProduct(id);

        auditLogService.log("DELETE_PRODUCT", String.valueOf(id),
                "Admin deleted product ID: " + id);

        log.info("Product ID: {} successfully deleted", id);
        return ResponseEntity.noContent().build();
    }
}