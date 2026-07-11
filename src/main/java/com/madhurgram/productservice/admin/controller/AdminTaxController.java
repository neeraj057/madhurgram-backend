package com.madhurgram.productservice.admin.controller;

import com.madhurgram.productservice.product.entity.HsnTaxMaster;
import com.madhurgram.productservice.product.repository.HsnTaxMasterRepository;
import com.madhurgram.productservice.audit.service.AuditLogService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/tax-slabs")
public class AdminTaxController {

    private final HsnTaxMasterRepository hsnTaxMasterRepository;
    private final AuditLogService auditLogService;

    public AdminTaxController(HsnTaxMasterRepository hsnTaxMasterRepository, AuditLogService auditLogService) {
        this.hsnTaxMasterRepository = hsnTaxMasterRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<HsnTaxMaster>> getAllTaxSlabs() {
        return ResponseEntity.ok(hsnTaxMasterRepository.findAll());
    }

    @PostMapping
    @CacheEvict(value = "products", allEntries = true)
    public ResponseEntity<HsnTaxMaster> addTaxSlab(@RequestBody HsnTaxMaster slab) {
        if (slab.getHsnCode() == null || slab.getHsnCode().trim().isEmpty()) {
            throw new IllegalArgumentException("HSN Code is mandatory.");
        }
        HsnTaxMaster saved = hsnTaxMasterRepository.save(slab);
        auditLogService.log("ADD_TAX_SLAB", slab.getHsnCode(), 
                "Added HSN tax slab: " + slab.getHsnCode() + " with rate: " + slab.getGstRate() + "%");
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{hsnCode}")
    @CacheEvict(value = {"products", "analytics"}, allEntries = true)
    public ResponseEntity<HsnTaxMaster> updateTaxSlab(@PathVariable String hsnCode, @RequestBody HsnTaxMaster slabDetails) {
        HsnTaxMaster slab = hsnTaxMasterRepository.findById(hsnCode)
                .orElseThrow(() -> new RuntimeException("HSN Tax slab not found: " + hsnCode));

        slab.setDescription(slabDetails.getDescription());
        slab.setGstRate(slabDetails.getGstRate());

        HsnTaxMaster updated = hsnTaxMasterRepository.save(slab);
        auditLogService.log("UPDATE_TAX_SLAB", hsnCode, 
                "Updated HSN tax slab: " + hsnCode + " rate to: " + slabDetails.getGstRate() + "%");
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{hsnCode}")
    @CacheEvict(value = "products", allEntries = true)
    public ResponseEntity<Void> deleteTaxSlab(@PathVariable String hsnCode) {
        if (!hsnTaxMasterRepository.existsById(hsnCode)) {
            throw new RuntimeException("HSN Tax slab not found: " + hsnCode);
        }
        hsnTaxMasterRepository.deleteById(hsnCode);
        auditLogService.log("DELETE_TAX_SLAB", hsnCode, 
                "Deleted HSN tax slab: " + hsnCode);
        return ResponseEntity.noContent().build();
    }
}
