package com.bank.feature.customers.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByUserId(UUID userId);

    @Query("select c from Customer c where lower(c.fullName) like lower(concat('%',:q,'%')) or c.phone like concat('%',:q,'%')")
    Page<Customer> search(@Param("q") String query, Pageable pageable);
}
