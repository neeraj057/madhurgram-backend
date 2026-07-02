package com.madhurgram.productservice.repository;

import com.madhurgram.productservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // भविष्य में फोन नंबर से ऑर्डर हिस्ट्री निकालने के लिए काम आएगा भाई
}