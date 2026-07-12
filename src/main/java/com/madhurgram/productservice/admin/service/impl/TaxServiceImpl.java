package com.madhurgram.productservice.admin.service.impl;

import com.madhurgram.productservice.admin.service.TaxService;
import com.madhurgram.productservice.product.dto.HsnTaxMasterDTO;
import com.madhurgram.productservice.product.entity.HsnTaxMaster;
import com.madhurgram.productservice.product.mapper.TaxMapper;
import com.madhurgram.productservice.product.repository.HsnTaxMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for administering Indian HSN tax slabs and GST slabs.
 */
@Slf4j
@Service
public class TaxServiceImpl implements TaxService {

    private final HsnTaxMasterRepository hsnTaxMasterRepository;
    private final TaxMapper taxMapper;

    /**
     * Constructor injection for TaxServiceImpl.
     *
     * @param hsnTaxMasterRepository HSN tax slabs repository
     * @param taxMapper              tax mapper instance
     */
    public TaxServiceImpl(HsnTaxMasterRepository hsnTaxMasterRepository, TaxMapper taxMapper) {
        this.hsnTaxMasterRepository = hsnTaxMasterRepository;
        this.taxMapper = taxMapper;
    }

    /**
     * Lists all registered HSN tax configurations.
     * Caches result to speed up queries.
     *
     * @return a list of HSN tax slabs DTO
     */
    @Override
    @Cacheable(value = "taxSlabs", key = "'all'")
    @Transactional(readOnly = true)
    public List<HsnTaxMasterDTO> getAllTaxSlabs() {
        log.info("[CACHE MISS] Fetching all HSN tax slabs from database...");
        List<HsnTaxMaster> list = hsnTaxMasterRepository.findAll();
        return list.stream()
                .map(taxMapper::toDTO)
                .toList();
    }

    /**
     * Registers a new HSN tax slab configuration.
     * Evicts outstanding tax and analytics caches.
     *
     * @param dto tax details payload DTO
     * @return created tax details DTO
     */
    @Override
    @Transactional
    @CacheEvict(value = {"taxSlabs", "products", "analytics"}, allEntries = true)
    public HsnTaxMasterDTO addTaxSlab(HsnTaxMasterDTO dto) {
        log.info("Registering new tax slab. HSN Code: '{}'", dto.getHsnCode());
        
        if (dto.getHsnCode() == null || dto.getHsnCode().trim().isEmpty()) {
            throw new IllegalArgumentException("HSN Code must not be blank.");
        }
        if (dto.getGstRate() == null || dto.getGstRate().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("GST rate cannot be negative.");
        }

        String hsn = dto.getHsnCode().trim();
        if (hsnTaxMasterRepository.existsById(hsn)) {
            log.warn("Tax slab registration aborted: HSN Code: '{}' already exists", hsn);
            throw new IllegalArgumentException("HSN slab already exists for code: " + hsn);
        }

        HsnTaxMaster entity = taxMapper.toEntity(dto);
        HsnTaxMaster saved = hsnTaxMasterRepository.save(entity);
        log.info("Successfully registered HSN tax slab: '{}'", saved.getHsnCode());
        return taxMapper.toDTO(saved);
    }

    /**
     * Updates an existing HSN tax slab properties.
     * Evicts active tax caches.
     *
     * @param hsnCode    target HSN code identifier
     * @param dtoDetails updated attributes
     * @return updated tax details DTO
     */
    @Override
    @Transactional
    @CacheEvict(value = {"taxSlabs", "products", "analytics"}, allEntries = true)
    public HsnTaxMasterDTO updateTaxSlab(String hsnCode, HsnTaxMasterDTO dtoDetails) {
        log.info("Updating tax slab configuration for HSN Code: '{}'", hsnCode);
        
        if (hsnCode == null || hsnCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Target HSN Code must not be empty.");
        }

        String cleanHsn = hsnCode.trim();
        HsnTaxMaster master = hsnTaxMasterRepository.findById(cleanHsn)
                .orElseThrow(() -> new IllegalArgumentException("HSN slab configuration not found for code: " + cleanHsn));

        if (dtoDetails.getGstRate() != null && dtoDetails.getGstRate().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("GST rate cannot be negative.");
        }

        master.setDescription(dtoDetails.getDescription() != null ? dtoDetails.getDescription().trim() : master.getDescription());
        if (dtoDetails.getGstRate() != null) {
            master.setGstRate(dtoDetails.getGstRate());
        }

        HsnTaxMaster updated = hsnTaxMasterRepository.save(master);
        log.info("Successfully updated HSN tax slab: '{}'", updated.getHsnCode());
        return taxMapper.toDTO(updated);
    }

    /**
     * Purges HSN tax configuration from database.
     * Evicts active tax caches.
     *
     * @param hsnCode target HSN code identifier
     */
    @Override
    @Transactional
    @CacheEvict(value = {"taxSlabs", "products", "analytics"}, allEntries = true)
    public void deleteTaxSlab(String hsnCode) {
        log.info("Deleting HSN tax slab configuration for code: '{}'", hsnCode);
        
        if (hsnCode == null || hsnCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Target HSN Code must not be empty.");
        }

        String cleanHsn = hsnCode.trim();
        if (!hsnTaxMasterRepository.existsById(cleanHsn)) {
            throw new IllegalArgumentException("HSN slab configuration not found for code: " + cleanHsn);
        }
        hsnTaxMasterRepository.deleteById(cleanHsn);
        log.info("Successfully deleted HSN tax slab: '{}'", cleanHsn);
    }
}
