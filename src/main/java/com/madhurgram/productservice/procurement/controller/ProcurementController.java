package com.madhurgram.productservice.procurement.controller;

import com.madhurgram.productservice.procurement.entity.PurchaseOrder;
import com.madhurgram.productservice.procurement.service.ProcurementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing admin purchase orders (POs) and inventory procurement.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/procurement")
@Tag(name = "Admin — Procurement", description = "Endpoints for managing inventory purchase orders and supplier updates")
public class ProcurementController {

    private final ProcurementService procurementService;

    /**
     * Constructor injection for ProcurementController.
     *
     * @param procurementService procurement service
     */
    public ProcurementController(ProcurementService procurementService) {
        this.procurementService = procurementService;
    }

    /**
     * Retrieves all purchase orders in the catalog.
     *
     * @return a list of purchase orders
     */
    @GetMapping("/pos")
    @Operation(summary = "List purchase orders", description = "Retrieves all purchase orders created for restocking inventory.")
    public ResponseEntity<List<PurchaseOrder>> getAllPOs() {
        log.info("Admin request: list all purchase orders");
        List<PurchaseOrder> pos = procurementService.getAllPurchaseOrders();
        log.info("Returning {} purchase order(s)", pos.size());
        return ResponseEntity.ok(pos);
    }

    /**
     * Modifies the details (e.g. quantity, supplier credentials) of an existing purchase order.
     *
     * @param id             purchase order ID
     * @param quantity       optional new order quantity
     * @param supplierName   optional updated supplier name
     * @param supplierEmail  optional updated supplier email
     * @return the updated purchase order
     */
    @PutMapping("/pos/{id}")
    @Operation(summary = "Update purchase order", description = "Modifies active purchase order variables before manager approval.")
    public ResponseEntity<PurchaseOrder> updatePO(
            @PathVariable Long id,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String supplierEmail
    ) {
        log.info("Admin request: update purchase order ID: {} (qty={}, supplier='{}')", id, quantity, supplierName);
        PurchaseOrder updated = procurementService.updatePurchaseOrder(id, quantity, supplierName, supplierEmail);
        log.info("Purchase order ID: {} successfully updated", id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Approves and executes a purchase order for dispatch.
     *
     * @param id purchase order ID to approve
     * @return the approved purchase order details
     */
    @PostMapping("/pos/{id}/approve")
    @Operation(summary = "Approve purchase order", description = "Approves a pending purchase order by ID to execute restocking.")
    public ResponseEntity<PurchaseOrder> approvePO(@PathVariable Long id) {
        log.info("Admin request: approve purchase order ID: {}", id);
        PurchaseOrder approved = procurementService.approvePurchaseOrder(id);
        log.info("Purchase order ID: {} successfully approved", id);
        return ResponseEntity.ok(approved);
    }
}
