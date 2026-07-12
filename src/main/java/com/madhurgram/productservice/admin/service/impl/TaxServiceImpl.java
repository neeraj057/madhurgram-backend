package com.madhurgram.productservice.admin.service.impl;

import com.madhurgram.productservice.admin.service.TaxService;
import com.madhurgram.productservice.product.dto.HsnTaxMasterDTO;
import com.madhurgram.productservice.product.entity.HsnTaxMaster;
import com.madhurgram.productservice.product.mapper.TaxMapper;
import com.madhurgram.productservice.product.repository.HsnTaxMasterRepository;
import com.madhurgram.productservice.audit.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaxServiceImpl implements TaxService {

    private final HsnTaxMasterRepository hsnTaxMasterRepository;
    private final TaxMapper taxMapper;
    private final AuditLogService auditLogService;

    public TaxServiceImpl(HsnTaxMasterRepository hsnTaxMasterRepository,
                          TaxMapper taxMapper,
                          AuditLogService auditLogService) {
        this.hsnTaxMasterRepository = hsnTaxMasterRepository;
        this.taxMapper = taxMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<HsnTaxMasterDTO> getAllTaxSlabs() {
        List<HsnTaxMaster> slabs = hsnTaxMasterRepository.findAll();
        return slabs.stream()
                .map(taxMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public HsnTaxMasterDTO addTaxSlab(HsnTaxMasterDTO dto) {
        if (dto.getHsnCode() == null || dto.getHsnCode().trim().isEmpty()) {
            throw new IllegalArgumentException("HSN Code is mandatory.");
        }
        
        HsnTaxMaster slab = taxMapper.toEntity(dto);
        HsnTaxMaster saved = hsnTaxMasterRepository.save(slab);
        auditLogService.log("ADD_TAX_SLAB", saved.getHsnCode(), 
                "Added HSN tax slab: " + saved.getHsnCode() + " with rate: " + saved.getGstRate() + "%");
        return taxMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public HsnTaxMasterDTO updateTaxSlab(String hsnCode, HsnTaxMasterDTO dtoDetails) {
        HsnTaxMaster slab = hsnTaxMasterRepository.findById(hsnCode)
                .orElseThrow(() -> new RuntimeException("HSN Tax slab not found: " + hsnCode));

        slab.setDescription(dtoDetails.getDescription());
        slab.setGstRate(dtoDetails.getGstRate());

        HsnTaxMaster updated = hsnTaxMasterRepository.save(slab);
        auditLogService.log("UPDATE_TAX_SLAB", hsnCode, 
                "Updated HSN tax slab: " + hsnCode + " rate to: " + dtoDetails.getGstRate() + "%");
        return taxMapper.toDTO(updated);
    }

    @Override
    @Transactional
    public void deleteTaxSlab(String hsnCode) {
        if (!hsnTaxMasterRepository.existsById(hsnCode)) {
            throw new RuntimeException("HSN Tax slab not found: " + hsnCode);
        }
        hsnTaxMasterRepository.deleteById(hsnCode);
        auditLogService.log("DELETE_TAX_SLAB", hsnCode, "Deleted HSN tax slab: " + hsnCode);
    }
}
