package com.bank.feature.wallets.web.dto;

import com.bank.feature.wallets.persistence.Wallet;
import com.bank.feature.wallets.persistence.WalletStatus;

import java.util.UUID;

public record WalletView(UUID id, UUID accountId, String currency, WalletStatus status) {

    public static WalletView of(Wallet w) {
        return new WalletView(w.getId(), w.getAccountId(), w.getCurrency(), w.getStatus());
    }
}
