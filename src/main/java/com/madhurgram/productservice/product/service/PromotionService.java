package com.madhurgram.productservice.product.service;

import com.madhurgram.productservice.common.entity.SystemSetting;
import com.madhurgram.productservice.common.repository.SystemSettingRepository;
import com.madhurgram.productservice.product.dto.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PromotionService {

    private final SystemSettingRepository systemSettingRepository;

    public PromotionService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    /**
     * Checks if the global Flash Sale is currently enabled in settings.
     * Caches the result to avoid database hits on every catalog request.
     */
    @Cacheable(value = "promotions", key = "'flash_sale_enabled'")
    public boolean isFlashSaleActive() {
        return systemSettingRepository.findById("FLASH_SALE_ENABLED")
                .map(SystemSetting::getSettingValue)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @Cacheable(value = "promotions", key = "'flash_sale_percentage'")
    public BigDecimal getFlashSalePercentage() {
        return systemSettingRepository.findById("FLASH_SALE_PERCENTAGE")
                .map(SystemSetting::getSettingValue)
                .map(val -> {
                    try {
                        return new BigDecimal(val).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                    } catch (Exception e) {
                        return new BigDecimal("0.15");
                    }
                })
                .orElse(new BigDecimal("0.15"));
    }

    @Cacheable(value = "promotions", key = "'flash_sale_category'")
    public String getFlashSaleTargetCategory() {
        return systemSettingRepository.findById("FLASH_SALE_CATEGORY")
                .map(SystemSetting::getSettingValue)
                .orElse("shop-all");
    }

    /**
     * Applies the active global promotions to a list of products.
     * Deep-copies the ProductDTOs to ensure the underlying cache remains untouched.
     */
    public List<ProductDTO> applyActivePromotions(List<ProductDTO> products) {
        if (!isFlashSaleActive()) {
            return products;
        }
        log.debug("Flash sale is active. Applying 15% discount to {} products", products.size());
        return products.stream()
                .map(this::applyDiscount)
                .collect(Collectors.toList());
    }

    /**
     * Applies the active global promotions to a paginated list of products.
     */
    public Page<ProductDTO> applyActivePromotions(Page<ProductDTO> productsPage) {
        if (!isFlashSaleActive()) {
            return productsPage;
        }
        log.debug("Flash sale is active. Applying 15% discount to paginated products");
        return productsPage.map(this::applyDiscount);
    }

    /**
     * Helper method to clone a DTO and slash its price by 15%.
     */
    private ProductDTO applyDiscount(ProductDTO product) {
        String targetCategory = getFlashSaleTargetCategory();
        
        // Ensure discount only applies to targeted category, or everything if shop-all
        if (!"shop-all".equalsIgnoreCase(targetCategory)) {
            if (product.getCategory() == null || !product.getCategory().equalsIgnoreCase(targetCategory)) {
                return product; // Skip product, does not match target category
            }
        }

        // Deep copy the builder fields to prevent mutating the original cached object
        ProductDTO discountedProduct = ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .volume(product.getVolume())
                .imageUrl(product.getImageUrl())
                .stock(product.getStock())
                .isActive(product.getIsActive())
                .category(product.getCategory())
                .tag(product.getTag())
                .rating(product.getRating())
                .hsnCode(product.getHsnCode())
                .hsnTaxMaster(product.getHsnTaxMaster())
                .showSalesCount(product.getShowSalesCount())
                .salesCount(product.getSalesCount())
                .realSalesCount(product.getRealSalesCount())
                .build();

        // Apply 15% Flash Sale Discount (Best Discount Wins Policy)
        if (discountedProduct.getPrice() != null && discountedProduct.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal basePrice = discountedProduct.getOriginalPrice() != null 
                ? discountedProduct.getOriginalPrice() 
                : discountedProduct.getPrice();
            
            if (basePrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentPrice = discountedProduct.getPrice();
                
                // Calculate current manual discount (e.g. 0.06 for 6%)
                BigDecimal manualDiscount = basePrice.subtract(currentPrice)
                        .divide(basePrice, 4, RoundingMode.HALF_UP);
                
                BigDecimal flashSaleDiscount = getFlashSalePercentage();
                
                // If the flash sale is a better deal than the manual discount
                if (manualDiscount.compareTo(flashSaleDiscount) < 0) {
                    discountedProduct.setOriginalPrice(basePrice);
                    BigDecimal discountFactor = BigDecimal.ONE.subtract(flashSaleDiscount);
                    BigDecimal newPrice = basePrice.multiply(discountFactor).setScale(0, RoundingMode.HALF_UP);
                    discountedProduct.setPrice(newPrice);
                }
            }
        }

        return discountedProduct;
    }
}
