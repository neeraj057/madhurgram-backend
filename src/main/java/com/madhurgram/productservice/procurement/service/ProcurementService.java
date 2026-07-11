package com.madhurgram.productservice.procurement.service;

import com.madhurgram.productservice.procurement.entity.PurchaseOrder;
import java.util.List;

public interface ProcurementService {
    List<PurchaseOrder> getAllPurchaseOrders();
    PurchaseOrder draftPurchaseOrder(Long productId, Integer quantity);
    PurchaseOrder updatePurchaseOrder(Long id, Integer quantity, String supplierName, String supplierEmail);
    PurchaseOrder approvePurchaseOrder(Long id);
}
