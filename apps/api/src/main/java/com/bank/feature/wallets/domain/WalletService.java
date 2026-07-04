package com.bank.feature.wallets.domain;

import com.bank.feature.wallets.persistence.Wallet;
import com.bank.feature.wallets.web.dto.MyWalletView;
import com.bank.feature.wallets.web.dto.WalletListItem;
import com.bank.feature.wallets.web.dto.WalletView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Port: wallet lifecycle and derived-balance reads. */
public interface WalletService {

    WalletView open(UUID accountId, String currency);

    /** All customer wallets (excludes system fee wallets), enriched with owner email. */
    List<WalletListItem> list();

    /** The signed-in user's own wallets (across their accounts), each with a derived balance. */
    List<MyWalletView> listMine(UUID ownerUserId);

    /** Derived balance (sum of ledger entries) — never a stored column. */
    BigDecimal balance(UUID walletId);

    WalletView freeze(UUID walletId);

    WalletView unfreeze(UUID walletId);

    /** Internal: fetch an ACTIVE wallet or throw (used by the money path). */
    Wallet getActive(UUID walletId);

    /**
     * Internal: the system fee-collection wallet for a currency, created on first
     * use. Fees credit this wallet so every fee leaves a balanced ledger trail.
     */
    UUID systemFeeWalletId(String currency);
}
