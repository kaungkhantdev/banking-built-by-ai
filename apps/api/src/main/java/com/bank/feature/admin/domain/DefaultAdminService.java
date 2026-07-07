package com.bank.feature.admin.domain;

import com.bank.feature.accounts.domain.AccountService;
import com.bank.feature.accounts.web.dto.AccountView;
import com.bank.feature.audit.domain.Audited;
import com.bank.feature.auth.persistence.User;
import com.bank.feature.auth.persistence.UserRepository;
import com.bank.feature.wallets.domain.WalletService;
import com.bank.feature.wallets.web.dto.WalletView;
import com.bank.shared.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DefaultAdminService implements AdminService {

    private final AccountService accounts;
    private final WalletService wallets;
    private final UserRepository users;

    public DefaultAdminService(AccountService accounts, WalletService wallets, UserRepository users) {
        this.accounts = accounts;
        this.wallets = wallets;
        this.users = users;
    }

    @Override
    @Transactional
    @Audited(action = "admin:freeze-account")
    public AccountView freezeAccount(UUID accountId) {
        return accounts.freeze(accountId);
    }

    @Override
    @Transactional
    @Audited(action = "admin:freeze-wallet")
    public WalletView freezeWallet(UUID walletId) {
        return wallets.freeze(walletId);
    }

    @Override
    @Transactional
    @Audited(action = "admin:unlock-user")
    public void unlockUser(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "Unknown user", 404));
        user.setEnabled(true);
        user.resetFailedLogins();
        users.save(user);
    }
}
