package com.madhurgram.productservice.product.mapper;

import com.madhurgram.productservice.product.dto.HsnTaxMasterDTO;
import com.madhurgram.productservice.product.dto.ProductDTO;
import com.madhurgram.productservice.product.entity.HsnTaxMaster;
import com.madhurgram.productservice.product.entity.Product;
import org.springframework.stereotype.Component;

/**
 * Mapper component for converting product catalog entities and tax configurations to DTO wrappers.
 */
@Component
public class ProductMapper {

    /**
     * Converts a Product entity to a ProductDTO.
     *
     * @param product the database entity
     * @return the product DTO
     */
    public ProductDTO toProductDTO(Product product) {
        if (product == null) {
            return null;
        }
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice() : product.getPrice())
                .volume(product.getVolume())
                .imageUrl(product.getImageUrl())
                .stock(product.getStock())
                .isActive(product.isActive())
                .category(product.getCategory())
                .tag(product.getTag())
                .rating(product.getRating() != null ? product.getRating() : java.math.BigDecimal.valueOf(4.8))
                .hsnCode(product.getHsnTaxMaster() != null ? product.getHsnTaxMaster().getHsnCode() : null)
                .hsnTaxMaster(toHsnTaxMasterDTO(product.getHsnTaxMaster()))
                .build();
    }

    /**
     * Converts an HsnTaxMaster entity to an HsnTaxMasterDTO.
     *
     * @param taxMaster the tax master database entity
     * @return the mapped HsnTaxMaster DTO
     */
    public HsnTaxMasterDTO toHsnTaxMasterDTO(HsnTaxMaster taxMaster) {
        if (taxMaster == null) {
            return null;
        }
        return HsnTaxMasterDTO.builder()
                .hsnCode(taxMaster.getHsnCode())
                .description(taxMaster.getDescription())
                .gstRate(taxMaster.getGstRate())
                .build();
    }
}
