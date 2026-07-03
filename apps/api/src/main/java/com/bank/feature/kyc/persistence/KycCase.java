package com.bank.feature.kyc.persistence;

import com.bank.shared.exception.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KYC case as an explicit state machine. Illegal jumps (e.g. REJECTED→VERIFIED)
 * are rejected by {@link #transitionTo}. REJECTED may re-submit documents.
 */
@Entity
@Table(name = "kyc_cases")
public class KycCase {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status;

    @Column
    private String vendorRef;

    @Column
    private String rejectReason;

    @Column(nullable = false)
    private Instant updatedAt;

    protected KycCase() {
    }

    public KycCase(UUID accountId) {
        this.accountId = accountId;
        this.status = KycStatus.CREATED;
        this.updatedAt = Instant.now();
    }

    private static final Map<KycStatus, Set<KycStatus>> ALLOWED = Map.of(
            KycStatus.CREATED, EnumSet.of(KycStatus.DOCS_SUBMITTED),
            KycStatus.DOCS_SUBMITTED, EnumSet.of(KycStatus.UNDER_REVIEW),
            KycStatus.UNDER_REVIEW, EnumSet.of(KycStatus.VERIFIED, KycStatus.REJECTED),
            KycStatus.VERIFIED, EnumSet.noneOf(KycStatus.class),
            KycStatus.REJECTED, EnumSet.of(KycStatus.DOCS_SUBMITTED));

    public void transitionTo(KycStatus next) {
        if (!ALLOWED.get(this.status).contains(next)) {
            throw new ApiException("KYC_ILLEGAL_TRANSITION",
                    "%s -> %s not allowed".formatted(status, next), 409);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public KycStatus getStatus() {
        return status;
    }

    public String getVendorRef() {
        return vendorRef;
    }

    public void setVendorRef(String vendorRef) {
        this.vendorRef = vendorRef;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
