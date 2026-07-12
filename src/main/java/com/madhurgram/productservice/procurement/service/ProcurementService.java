package com.madhurgram.productservice.procurement.service;

import com.madhurgram.productservice.procurement.dto.PurchaseOrderDTO;
import java.util.List;

public interface ProcurementService {
    List<PurchaseOrderDTO> getAllPurchaseOrders();
    PurchaseOrderDTO draftPurchaseOrder(Long productId, Integer quantity);
    PurchaseOrderDTO updatePurchaseOrder(Long id, Integer quantity, String supplierName, String supplierEmail);
    PurchaseOrderDTO approvePurchaseOrder(Long id);
}
