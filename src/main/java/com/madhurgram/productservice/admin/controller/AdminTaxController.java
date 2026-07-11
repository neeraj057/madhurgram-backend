package com.madhurgram.productservice.admin.controller;

import com.madhurgram.productservice.product.entity.HsnTaxMaster;
import com.madhurgram.productservice.product.repository.HsnTaxMasterRepository;
import com.madhurgram.productservice.audit.service.AuditLogService;

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
@RequestMapping("/api/admin/tax-slabs")
@Tag(name = "Admin — Tax Settings", description = "Tax slab mappings and HSN code GST percentage settings")
public class AdminTaxController {

    private final HsnTaxMasterRepository hsnTaxMasterRepository;
    private final AuditLogService auditLogService;

    /**
     * Constructor injection for AdminTaxController.
     *
     * @param hsnTaxMasterRepository database access for HSN codes
     * @param auditLogService        audit log manager
     */
    public AdminTaxController(HsnTaxMasterRepository hsnTaxMasterRepository, AuditLogService auditLogService) {
        this.hsnTaxMasterRepository = hsnTaxMasterRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Retrieves all HSN tax configurations.
     *
     * @return a list of HSN tax slabs
     */
    @GetMapping
    @Operation(summary = "List tax slabs", description = "Retrieves all HSN tax code mappings and GST rates.")
    public ResponseEntity<List<HsnTaxMaster>> getAllTaxSlabs() {
        log.info("Admin request: list all tax slabs");
        List<HsnTaxMaster> slabs = hsnTaxMasterRepository.findAll();
        log.info("Returning {} tax slab(s)", slabs.size());
        return ResponseEntity.ok(slabs);
    }

    /**
     * Registers a new HSN tax rate mapping.
     * Evicts active product catalog cache.
     *
     * @param slab the tax mapping payload details
     * @return the created HSN record details
     */
    @PostMapping
    @CacheEvict(value = "products", allEntries = true)
    @Operation(summary = "Add tax slab", description = "Creates a new HSN code mapping ruleset and writes an audit event.")
    public ResponseEntity<HsnTaxMaster> addTaxSlab(@RequestBody HsnTaxMaster slab) {
        log.info("Admin request: create tax slab HSN: '{}' (GST Rate: {}%)", slab.getHsnCode(), slab.getGstRate());

        if (slab.getHsnCode() == null || slab.getHsnCode().trim().isEmpty()) {
            log.warn("Create tax slab failed: HSN Code is blank");
            throw new IllegalArgumentException("HSN Code is mandatory.");
        }

        HsnTaxMaster saved = hsnTaxMasterRepository.save(slab);
        auditLogService.log("ADD_TAX_SLAB", slab.getHsnCode(),
                "Added HSN tax slab: " + slab.getHsnCode() + " with rate: " + slab.getGstRate() + "%");

        log.info("Tax slab successfully registered for HSN: '{}'", saved.getHsnCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Modifies an existing HSN tax slab description or rate.
     * Evicts catalog and dashboard analytics caches.
     *
     * @param hsnCode     target HSN identifier
     * @param slabDetails updated parameters payload
     * @return the updated HSN record details
     */
    @PutMapping("/{hsnCode}")
    @CacheEvict(value = { "products", "analytics" }, allEntries = true)
    @Operation(summary = "Update tax slab", description = "Modifies description/GST rates of an existing HSN code mapping.")
    public ResponseEntity<HsnTaxMaster> updateTaxSlab(@PathVariable String hsnCode,
            @RequestBody HsnTaxMaster slabDetails) {
        log.info("Admin request: update tax slab HSN: '{}'", hsnCode);

        HsnTaxMaster slab = hsnTaxMasterRepository.findById(hsnCode)
                .orElseThrow(() -> {
                    log.warn("Update tax slab failed: HSN: '{}' not found", hsnCode);
                    return new RuntimeException("HSN Tax slab not found: " + hsnCode);
                });

        slab.setDescription(slabDetails.getDescription());
        slab.setGstRate(slabDetails.getGstRate());

        HsnTaxMaster updated = hsnTaxMasterRepository.save(slab);
        auditLogService.log("UPDATE_TAX_SLAB", hsnCode,
                "Updated HSN tax slab: " + hsnCode + " rate to: " + slabDetails.getGstRate() + "%");

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

        if (!hsnTaxMasterRepository.existsById(hsnCode)) {
            log.warn("Delete tax slab failed: HSN: '{}' not found", hsnCode);
            throw new RuntimeException("HSN Tax slab not found: " + hsnCode);
        }

        hsnTaxMasterRepository.deleteById(hsnCode);
        auditLogService.log("DELETE_TAX_SLAB", hsnCode, "Deleted HSN tax slab: " + hsnCode);

        log.info("Tax slab HSN: '{}' successfully deleted", hsnCode);
        return ResponseEntity.noContent().build();
    }
}
