package com.bank.feature.accounts.persistence;

/** Account lifecycle. PENDING until KYC; ACTIVE to operate; FROZEN/CLOSED by ops. */
public enum AccountStatus {
    PENDING, ACTIVE, FROZEN, CLOSED
}
