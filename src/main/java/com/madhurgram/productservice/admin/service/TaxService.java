package com.madhurgram.productservice.admin.service;

import com.madhurgram.productservice.product.dto.HsnTaxMasterDTO;
import java.util.List;

public interface TaxService {
    // list of all tax slabs
    List<HsnTaxMasterDTO> getAllTaxSlabs();

    // add new tax slab
    HsnTaxMasterDTO addTaxSlab(HsnTaxMasterDTO dto);

    // update tax slab
    HsnTaxMasterDTO updateTaxSlab(String hsnCode, HsnTaxMasterDTO dtoDetails);

    // delete tax slab
    void deleteTaxSlab(String hsnCode);
}
