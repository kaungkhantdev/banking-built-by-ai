package com.bank.feature.customers.domain;

import com.bank.feature.audit.domain.Audited;
import com.bank.feature.customers.persistence.Customer;
import com.bank.feature.customers.persistence.CustomerRepository;
import com.bank.feature.customers.persistence.CustomerStatus;
import com.bank.feature.customers.web.dto.CustomerView;
import com.bank.shared.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class DefaultCustomerService implements CustomerService {

    private final CustomerRepository customers;

    public DefaultCustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    @Transactional
    @Audited(action = "customer:create")
    public CustomerView create(UUID userId, String fullName, String phone, LocalDate dateOfBirth) {
        if (customers.findByUserId(userId).isPresent()) {
            throw new ApiException("CUSTOMER_DUPLICATE", "A profile already exists for this user", 409);
        }
        return CustomerView.of(customers.save(new Customer(userId, fullName, phone, dateOfBirth)));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerView get(UUID id) {
        return CustomerView.of(require(id));
    }

    @Override
    @Transactional
    @Audited(action = "customer:update")
    public CustomerView update(UUID id, String fullName, String phone) {
        Customer c = require(id);
        if (fullName != null) c.setFullName(fullName);
        if (phone != null) c.setPhone(phone);
        return CustomerView.of(c);
    }

    @Override
    @Transactional
    @Audited(action = "customer:suspend")
    public CustomerView suspend(UUID id) {
        Customer c = require(id);
        if (c.getStatus() != CustomerStatus.ACTIVE) {
            throw new ApiException("CUSTOMER_ILLEGAL_TRANSITION",
                    "Only ACTIVE customers can be suspended (was " + c.getStatus() + ")", 409);
        }
        c.setStatus(CustomerStatus.SUSPENDED);
        return CustomerView.of(c);
    }

    @Override
    @Transactional
    @Audited(action = "customer:close")
    public CustomerView close(UUID id) {
        Customer c = require(id);
        c.setStatus(CustomerStatus.CLOSED);
        return CustomerView.of(c);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerView> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return customers.findAll(pageable).map(CustomerView::of);
        }
        return customers.search(query, pageable).map(CustomerView::of);
    }

    private Customer require(UUID id) {
        return customers.findById(id)
                .orElseThrow(() -> new ApiException("CUSTOMER_NOT_FOUND", "Unknown customer", 404));
    }
}
