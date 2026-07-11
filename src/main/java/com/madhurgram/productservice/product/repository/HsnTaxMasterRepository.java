package com.madhurgram.productservice.product.repository;

import com.madhurgram.productservice.product.entity.HsnTaxMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HsnTaxMasterRepository extends JpaRepository<HsnTaxMaster, String> {
}
