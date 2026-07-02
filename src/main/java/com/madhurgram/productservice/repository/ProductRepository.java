package com.madhurgram.productservice.repository;

import com.madhurgram.productservice.entity.Product;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Category wise dynamic product filter query
    List<Product> findByCategoryAndIsActiveTrue(String category);

    // Default fetch all active products
    List<Product> findByIsActiveTrue();

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :productId AND p.stock >= :quantity")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock <= 5 AND p.isActive = true")
    Long getLowStockCount();
}