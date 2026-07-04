package com.madhurgram.productservice.repository;

import com.madhurgram.productservice.entity.Order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByPhoneNumberOrderByOrderDateDesc(String phoneNumber);

    // 📊 आज का कुल रेवेन्यू (सिर्फ उन ऑर्डर्स का जो CANCELLED नहीं हैं)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE FUNCTION('DATE', o.orderDate) = CURRENT_DATE AND o.orderStatus != 'CANCELLED'")
    java.math.BigDecimal getTodayRevenue();

    // 📈 आज आए कुल ऑर्डर्स की संख्या
    @Query("SELECT COUNT(o) FROM Order o WHERE FUNCTION('DATE', o.orderDate) = CURRENT_DATE")
    Long getTodayOrderCount();

    // ⏳ कितने ऑर्डर्स अभी PENDING स्टेट में बैठे हैं
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = 'PENDING'")
    Long getPendingOrderCount();

    List<Order> findByPhoneNumber(String phoneNumber);
}