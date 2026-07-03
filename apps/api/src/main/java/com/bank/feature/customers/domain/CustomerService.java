package com.bank.feature.customers.domain;

import com.bank.feature.customers.web.dto.CustomerView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface CustomerService {

    CustomerView create(UUID userId, String fullName, String phone, LocalDate dateOfBirth);

    CustomerView get(UUID id);

    CustomerView update(UUID id, String fullName, String phone);

    CustomerView suspend(UUID id);

    CustomerView close(UUID id);

    Page<CustomerView> search(String query, Pageable pageable);
}
