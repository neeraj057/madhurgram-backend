package com.madhurgram.productservice.procurement.controller;

import com.madhurgram.productservice.procurement.entity.PurchaseOrder;
import com.madhurgram.productservice.procurement.service.ProcurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/procurement")
public class ProcurementController {

    private final ProcurementService procurementService;

    public ProcurementController(ProcurementService procurementService) {
        this.procurementService = procurementService;
    }

    @GetMapping("/pos")
    public ResponseEntity<List<PurchaseOrder>> getAllPOs() {
        return ResponseEntity.ok(procurementService.getAllPurchaseOrders());
    }

    @PutMapping("/pos/{id}")
    public ResponseEntity<PurchaseOrder> updatePO(
            @PathVariable Long id,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String supplierEmail
    ) {
        return ResponseEntity.ok(procurementService.updatePurchaseOrder(id, quantity, supplierName, supplierEmail));
    }

    @PostMapping("/pos/{id}/approve")
    public ResponseEntity<PurchaseOrder> approvePO(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.approvePurchaseOrder(id));
    }
}
