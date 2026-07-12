package com.madhurgram.productservice.procurement.mapper;

import com.madhurgram.productservice.procurement.dto.PurchaseOrderDTO;
import com.madhurgram.productservice.procurement.entity.PurchaseOrder;
import com.madhurgram.productservice.product.mapper.ProductMapper;
import org.springframework.stereotype.Component;

/**
 * Mapper component to map inventory restocking purchase orders and supplier configurations.
 */
@Component
public class ProcurementMapper {

    private final ProductMapper productMapper;

    /**
     * Constructor injection for ProcurementMapper.
     *
     * @param productMapper product catalog mapper dependency
     */
    public ProcurementMapper(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /**
     * Converts a PurchaseOrder entity to a PurchaseOrderDTO.
     *
     * @param po the purchase order database entity
     * @return the mapped purchase order DTO
     */
    public PurchaseOrderDTO toDTO(PurchaseOrder po) {
        if (po == null) {
            return null;
        }
        return PurchaseOrderDTO.builder()
                .id(po.getId())
                .product(productMapper.toProductDTO(po.getProduct()))
                .quantity(po.getQuantity())
                .supplierName(po.getSupplierName())
                .supplierEmail(po.getSupplierEmail())
                .status(po.getStatus())
                .createdAt(po.getCreatedAt())
                .approvedAt(po.getApprovedAt())
                .build();
    }
}
