package com.madhurgram.productservice.bundle.service;

import com.madhurgram.productservice.bundle.dto.BundleRequestDTO;
import com.madhurgram.productservice.bundle.dto.BundleResponseDTO;
import com.madhurgram.productservice.bundle.dto.BundleResponseDTO.BundleItemDTO;
import com.madhurgram.productservice.bundle.dto.PublicFooterSectionDTO;
import com.madhurgram.productservice.bundle.entity.Bundle;
import com.madhurgram.productservice.bundle.entity.BundleItem;
import com.madhurgram.productservice.bundle.repository.BundleRepository;
import com.madhurgram.productservice.common.entity.SystemSetting;
import com.madhurgram.productservice.common.repository.SystemSettingRepository;
import com.madhurgram.productservice.product.entity.Product;
import com.madhurgram.productservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BundleServiceImpl implements BundleService {

    private static final String FOOTER_MODE_KEY = "footer.section.mode";
    private static final String DEFAULT_FOOTER_MODE = "BRAND_STORY";

    private final BundleRepository bundleRepository;
    private final ProductRepository productRepository;
    private final SystemSettingRepository systemSettingRepository;

    // ─────────────────────── Public API ───────────────────────

    @Override
    @Transactional(readOnly = true)
    public PublicFooterSectionDTO getPublicFooterSection() {
        String mode = getFooterMode();
        List<BundleResponseDTO> activeBundles = bundleRepository
                .findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toDTO)
                .toList();
        return new PublicFooterSectionDTO(mode, activeBundles);
    }

    // ─────────────────────── Admin API ───────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<BundleResponseDTO> getAllBundles() {
        return bundleRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public BundleResponseDTO createBundle(BundleRequestDTO request) {
        Bundle bundle = Bundle.builder()
                .tabName(request.tabName())
                .name(request.name())
                .description(request.description())
                .discountPercent(request.discountPercent())
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .active(false) // always starts inactive — admin explicitly activates
                .build();

        addItemsToBundle(bundle, request.productIds());
        Bundle saved = bundleRepository.save(bundle);
        log.info("Bundle created: id={}, name={}", saved.getId(), saved.getName());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public BundleResponseDTO updateBundle(Long id, BundleRequestDTO request) {
        Bundle bundle = bundleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bundle not found: " + id));

        bundle.setTabName(request.tabName());
        bundle.setName(request.name());
        bundle.setDescription(request.description());
        bundle.setDiscountPercent(request.discountPercent());
        if (request.displayOrder() != null) bundle.setDisplayOrder(request.displayOrder());

        // Replace all items
        bundle.getItems().clear();
        addItemsToBundle(bundle, request.productIds());

        Bundle saved = bundleRepository.save(bundle);
        log.info("Bundle updated: id={}", id);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public BundleResponseDTO toggleActive(Long id) {
        Bundle bundle = bundleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bundle not found: " + id));
        bundle.setActive(!bundle.isActive());
        Bundle saved = bundleRepository.save(bundle);
        log.info("Bundle {} toggled to active={}", id, saved.isActive());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public void deleteBundle(Long id) {
        if (!bundleRepository.existsById(id)) {
            throw new NoSuchElementException("Bundle not found: " + id);
        }
        bundleRepository.deleteById(id);
        log.info("Bundle deleted: id={}", id);
    }

    @Override
    public String getFooterMode() {
        return systemSettingRepository.findById(FOOTER_MODE_KEY)
                .map(SystemSetting::getSettingValue)
                .orElse(DEFAULT_FOOTER_MODE);
    }

    @Override
    @Transactional
    public void setFooterMode(String mode) {
        if (!mode.equals("COMBOS") && !mode.equals("BRAND_STORY")) {
            throw new IllegalArgumentException("footer mode must be COMBOS or BRAND_STORY");
        }
        SystemSetting setting = systemSettingRepository.findById(FOOTER_MODE_KEY)
                .orElse(SystemSetting.builder()
                        .settingKey(FOOTER_MODE_KEY)
                        .description("Controls what the homepage footer section displays")
                        .build());
        setting.setSettingValue(mode);
        systemSettingRepository.save(setting);
        log.info("Footer section mode set to: {}", mode);
    }

    // ─────────────────────── Helpers ───────────────────────

    private void addItemsToBundle(Bundle bundle, List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            List<Long> foundIds = products.stream().map(Product::getId).toList();
            List<Long> missingIds = productIds.stream().filter(pid -> !foundIds.contains(pid)).toList();
            throw new IllegalArgumentException("Products not found in database: " + missingIds);
        }
        products.forEach(product -> {
            BundleItem item = BundleItem.builder()
                    .bundle(bundle)
                    .product(product)
                    .build();
            bundle.getItems().add(item);
        });
    }

    private BundleResponseDTO toDTO(Bundle bundle) {
        List<BundleItemDTO> itemDTOs = bundle.getItems().stream()
                .map(item -> {
                    Product p = item.getProduct();
                    return new BundleItemDTO(
                            p.getId(),
                            p.getName(),
                            p.getPrice(),
                            p.getVolume(),
                            p.getImageUrl(),
                            p.getStock()
                    );
                })
                .toList();

        // Calculate prices from real DB data
        BigDecimal originalPrice = itemDTOs.stream()
                .map(BundleItemDTO::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountMultiplier = BigDecimal.ONE
                .subtract(BigDecimal.valueOf(bundle.getDiscountPercent()).divide(BigDecimal.valueOf(100)));

        BigDecimal bundlePrice = originalPrice.multiply(discountMultiplier)
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal savings = originalPrice.subtract(bundlePrice);

        return new BundleResponseDTO(
                bundle.getId(),
                bundle.getTabName(),
                bundle.getName(),
                bundle.getDescription(),
                bundle.getDiscountPercent(),
                bundle.isActive(),
                bundle.getDisplayOrder(),
                originalPrice,
                bundlePrice,
                savings,
                itemDTOs
        );
    }
}
