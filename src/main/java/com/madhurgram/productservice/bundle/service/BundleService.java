package com.madhurgram.productservice.bundle.service;

import com.madhurgram.productservice.bundle.dto.BundleRequestDTO;
import com.madhurgram.productservice.bundle.dto.BundleResponseDTO;
import com.madhurgram.productservice.bundle.dto.PublicFooterSectionDTO;

import java.util.List;

public interface BundleService {

    /** Admin: get all bundles (active + inactive) */
    List<BundleResponseDTO> getAllBundles();

    /** Admin: create a new bundle */
    BundleResponseDTO createBundle(BundleRequestDTO request);

    /** Admin: update an existing bundle */
    BundleResponseDTO updateBundle(Long id, BundleRequestDTO request);

    /** Admin: toggle active/inactive */
    BundleResponseDTO toggleActive(Long id);

    /** Admin: delete a bundle */
    void deleteBundle(Long id);

    /** Admin: get current footer mode */
    String getFooterMode();

    /** Admin: set footer mode (COMBOS or BRAND_STORY) */
    void setFooterMode(String mode);

    /** Public/Storefront: get footer mode + active bundles in one call */
    PublicFooterSectionDTO getPublicFooterSection();
}
