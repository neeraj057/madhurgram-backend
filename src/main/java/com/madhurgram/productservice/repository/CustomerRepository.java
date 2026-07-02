package com.madhurgram.productservice.repository;

import com.madhurgram.productservice.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 🔒 Production Standard: LEFT JOIN FETCH use karne se addresses ek hi query me load ho jayenge, LazyInitializationException nahi aayega
    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.addresses WHERE c.phoneNumber = :phone")
    Optional<Customer> findByPhoneNumberWithAddresses(@Param("phone") String phone);

    Optional<Customer> findByPhoneNumber(String phoneNumber);
}