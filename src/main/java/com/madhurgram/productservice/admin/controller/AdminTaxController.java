package com.madhurgram.productservice.admin.controller;

import com.madhurgram.productservice.admin.service.TaxService;
import com.madhurgram.productservice.product.dto.HsnTaxMasterDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing HSN code taxes, GST rates, and slab definitions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/tax-slabs")
@Tag(name = "Admin — Tax Settings", description = "Tax slab mappings and HSN code GST percentage settings")
public class AdminTaxController {

    private final TaxService taxService;

    /**
     * Constructor injection for AdminTaxController.
     *
     * @param taxService tax management service
     */
    public AdminTaxController(TaxService taxService) {
        this.taxService = taxService;
    }

    /**
     * Retrieves all HSN tax configurations.
     *
     * @return a list of HSN tax slabs
     */
    @GetMapping
    @Operation(summary = "List tax slabs", description = "Retrieves all HSN tax code mappings and GST rates.")
    public ResponseEntity<List<HsnTaxMasterDTO>> getAllTaxSlabs() {
        log.info("Admin request: list all tax slabs");
        List<HsnTaxMasterDTO> dtos = taxService.getAllTaxSlabs();
        log.info("Returning {} tax slab(s)", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Registers a new HSN tax rate mapping.
     * Evicts active product catalog cache.
     *
     * @param dto the tax mapping payload details
     * @return the created HSN record details
     */
    @PostMapping
    @CacheEvict(value = "products", allEntries = true)
    @Operation(summary = "Add tax slab", description = "Creates a new HSN code mapping ruleset and writes an audit event.")
    public ResponseEntity<HsnTaxMasterDTO> addTaxSlab(@RequestBody HsnTaxMasterDTO dto) {
        log.info("Admin request: create tax slab HSN: '{}' (GST Rate: {}%)", dto.getHsnCode(), dto.getGstRate());
        HsnTaxMasterDTO saved = taxService.addTaxSlab(dto);
        log.info("Tax slab successfully registered for HSN: '{}'", saved.getHsnCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Modifies an existing HSN tax slab description or rate.
     * Evicts catalog and dashboard analytics caches.
     *
     * @param hsnCode    target HSN identifier
     * @param dtoDetails updated parameters payload
     * @return the updated HSN record details
     */
    @PutMapping("/{hsnCode}")
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    @Operation(summary = "Update tax slab", description = "Modifies description/GST rates of an existing HSN code mapping.")
    public ResponseEntity<HsnTaxMasterDTO> updateTaxSlab(@PathVariable String hsnCode, @RequestBody HsnTaxMasterDTO dtoDetails) {
        log.info("Admin request: update tax slab HSN: '{}'", hsnCode);
        HsnTaxMasterDTO updated = taxService.updateTaxSlab(hsnCode, dtoDetails);
        log.info("Tax slab HSN: '{}' successfully updated", hsnCode);
        return ResponseEntity.ok(updated);
    }

    /**
     * Purges HSN tax mappings.
     * Evicts active product catalog cache.
     *
     * @param hsnCode target HSN code to delete
     * @return HTTP status 204 (No Content)
     */
    @DeleteMapping("/{hsnCode}")
    @CacheEvict(value = "products", allEntries = true)
    @Operation(summary = "Delete tax slab", description = "Deletes an HSN tax mapping by key ID and logs an admin audit event.")
    public ResponseEntity<Void> deleteTaxSlab(@PathVariable String hsnCode) {
        log.info("Admin request: delete tax slab HSN: '{}'", hsnCode);
        taxService.deleteTaxSlab(hsnCode);
        log.info("Tax slab HSN: '{}' successfully deleted", hsnCode);
        return ResponseEntity.noContent().build();
    }
}
