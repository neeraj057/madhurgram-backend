package com.madhurgram.productservice.product.mapper;

import com.madhurgram.productservice.product.dto.HsnTaxMasterDTO;
import com.madhurgram.productservice.product.entity.HsnTaxMaster;
import org.springframework.stereotype.Component;

/**
 * Mapper component for bidirectional mapping between HSN tax entities and DTO wrappers.
 */
@Component
public class TaxMapper {

    /**
     * Converts an HsnTaxMaster entity to an HsnTaxMasterDTO.
     *
     * @param slab the database entity
     * @return the tax DTO
     */
    public HsnTaxMasterDTO toDTO(HsnTaxMaster slab) {
        if (slab == null) {
            return null;
        }
        return HsnTaxMasterDTO.builder()
                .hsnCode(slab.getHsnCode())
                .description(slab.getDescription())
                .gstRate(slab.getGstRate())
                .build();
    }

    /**
     * Converts an HsnTaxMasterDTO to an HsnTaxMaster entity.
     *
     * @param dto the tax DTO
     * @return the database entity
     */
    public HsnTaxMaster toEntity(HsnTaxMasterDTO dto) {
        if (dto == null) {
            return null;
        }
        return HsnTaxMaster.builder()
                .hsnCode(dto.getHsnCode())
                .description(dto.getDescription())
                .gstRate(dto.getGstRate())
                .build();
    }
}
