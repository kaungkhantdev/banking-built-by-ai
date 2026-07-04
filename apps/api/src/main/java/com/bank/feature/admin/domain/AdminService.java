package com.bank.feature.admin.domain;

import com.bank.feature.accounts.web.dto.AccountView;
import com.bank.feature.wallets.web.dto.WalletView;

import java.util.UUID;

/** Back-office mutations. Every method is audited (FR-21.5). */
public interface AdminService {

    AccountView freezeAccount(UUID accountId);

    WalletView freezeWallet(UUID walletId);

    void unlockUser(UUID userId);
}
