package com.madhurgram.productservice.bundle.repository;

import com.madhurgram.productservice.bundle.entity.Bundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BundleRepository extends JpaRepository<Bundle, Long> {

    /** Fetch only active bundles sorted by display order — used by storefront */
    List<Bundle> findByActiveTrueOrderByDisplayOrderAsc();

    /** Fetch all bundles sorted by display order — used by admin */
    List<Bundle> findAllByOrderByDisplayOrderAsc();
}
