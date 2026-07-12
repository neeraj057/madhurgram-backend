package com.madhurgram.productservice.admin.service;

import com.madhurgram.productservice.product.dto.HsnTaxMasterDTO;
import java.util.List;

public interface TaxService {
    List<HsnTaxMasterDTO> getAllTaxSlabs();
    HsnTaxMasterDTO addTaxSlab(HsnTaxMasterDTO dto);
    HsnTaxMasterDTO updateTaxSlab(String hsnCode, HsnTaxMasterDTO dtoDetails);
    void deleteTaxSlab(String hsnCode);
}
