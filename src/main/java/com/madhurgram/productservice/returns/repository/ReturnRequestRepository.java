package com.madhurgram.productservice.returns.repository;

import com.madhurgram.productservice.returns.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByCustomerPhone(String customerPhone);
    Optional<ReturnRequest> findByOrderId(Long orderId);
}
