package com.bank.feature.customers.web.dto;

import com.bank.feature.customers.persistence.Customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerView(
        UUID id,
        UUID userId,
        String fullName,
        String phone,
        String dateOfBirth,
        String status,
        Instant createdAt
) {
    public static CustomerView of(Customer c) {
        return new CustomerView(
                c.getId(), c.getUserId(), c.getFullName(), c.getPhone(),
                c.getDateOfBirth() != null ? c.getDateOfBirth().toString() : null,
                c.getStatus().name(), c.getCreatedAt());
    }
}
