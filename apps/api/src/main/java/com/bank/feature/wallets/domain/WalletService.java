package com.bank.feature.wallets.domain;

import com.bank.feature.wallets.persistence.Wallet;
import com.bank.feature.wallets.web.dto.WalletView;

import java.math.BigDecimal;
import java.util.UUID;

/** Port: wallet lifecycle and derived-balance reads. */
public interface WalletService {

    WalletView open(UUID accountId, String currency);

    /** Derived balance (sum of ledger entries) — never a stored column. */
    BigDecimal balance(UUID walletId);

    WalletView freeze(UUID walletId);

    WalletView unfreeze(UUID walletId);

    /** Internal: fetch an ACTIVE wallet or throw (used by the money path). */
    Wallet getActive(UUID walletId);
}
