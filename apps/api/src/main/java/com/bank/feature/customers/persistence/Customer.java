package com.bank.feature.customers.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer extends BaseAuditEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String fullName;

    @Column
    private String phone;

    @Column
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;

    protected Customer() {}

    public Customer(UUID userId, String fullName, String phone, LocalDate dateOfBirth) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.status = CustomerStatus.ACTIVE;
    }

    public UUID getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public CustomerStatus getStatus() { return status; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setStatus(CustomerStatus status) { this.status = status; }
}
