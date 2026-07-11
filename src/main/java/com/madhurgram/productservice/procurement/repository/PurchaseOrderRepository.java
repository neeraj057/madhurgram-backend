package com.madhurgram.productservice.procurement.repository;

import com.madhurgram.productservice.procurement.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByStatus(String status);
    Optional<PurchaseOrder> findByProductIdAndStatus(Long productId, String status);
}
