package com.bank.feature.wallets.persistence;

/** Wallet lifecycle. FROZEN blocks money-out without deleting anything. */
public enum WalletStatus {
    ACTIVE, FROZEN, CLOSED
}
