package com.madhurgram.productservice.order.repository;

import com.madhurgram.productservice.order.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByPhoneNumberOrderByOrderDateDesc(String phoneNumber);

    // 📊 आज का कुल रेवेन्यू (सिर्फ उन ऑर्डर्स का जो CANCELLED नहीं हैं)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate >= :start AND o.orderDate <= :end AND o.orderStatus != com.madhurgram.productservice.order.entity.OrderStatus.CANCELLED")
    java.math.BigDecimal getTodayRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 📈 आज आए कुल ऑर्डर्स की संख्या
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :start AND o.orderDate <= :end")
    Long getTodayOrderCount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ⏳ कितने ऑर्डर्स अभी PENDING स्टेट में बैठे हैं
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = com.madhurgram.productservice.order.entity.OrderStatus.PENDING")
    Long getPendingOrderCount();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems")
    List<Order> findAllWithItems();

    List<Order> findByPhoneNumber(String phoneNumber);

    List<Order> findByOrderDateAfter(LocalDateTime since);

    @Query("SELECT DISTINCT o.phoneNumber FROM Order o")
    List<String> findDistinctPhoneNumbers();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems")
    Page<Order> findAllWithItems(Pageable pageable);

    @Query(value = "SELECT DISTINCT o.phone_number FROM orders o", 
           countQuery = "SELECT COUNT(DISTINCT o.phone_number) FROM orders o", 
           nativeQuery = true)
    Page<String> findDistinctPhoneNumbers(Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.phoneNumber IN :phoneNumbers")
    List<Order> findOrdersWithItemsByPhoneNumbers(@Param("phoneNumbers") List<String> phoneNumbers);

    @Query("SELECT o.phoneNumber FROM Order o WHERE o.orderDate >= :since GROUP BY o.phoneNumber ORDER BY SUM(o.totalAmount) DESC")
    List<String> findTopSpenderPhoneNumbersSince(@Param("since") LocalDateTime since);

    @Query("SELECT o.phoneNumber FROM Order o GROUP BY o.phoneNumber HAVING MAX(o.orderDate) < :since")
    List<String> findInactiveCustomerPhoneNumbersBefore(@Param("since") LocalDateTime since);

    @Query("SELECT DISTINCT o.phoneNumber FROM Order o JOIN o.orderItems i WHERE o.orderDate >= :since AND LOWER(i.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<String> findCustomerPhoneNumbersByProductKeywordSince(@Param("keyword") String keyword, @Param("since") LocalDateTime since);
}