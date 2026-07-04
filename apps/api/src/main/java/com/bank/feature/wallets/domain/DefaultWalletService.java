package com.bank.feature.wallets.domain;

import com.bank.feature.accounts.persistence.Account;
import com.bank.feature.accounts.persistence.AccountRepository;
import com.bank.feature.auth.persistence.User;
import com.bank.feature.auth.persistence.UserRepository;
import com.bank.feature.ledger.domain.LedgerService;
import com.bank.feature.wallets.persistence.Wallet;
import com.bank.feature.wallets.persistence.WalletRepository;
import com.bank.feature.wallets.persistence.WalletStatus;
import com.bank.feature.wallets.web.dto.MyWalletView;
import com.bank.feature.wallets.web.dto.WalletListItem;
import com.bank.feature.wallets.web.dto.WalletView;
import com.bank.shared.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DefaultWalletService implements WalletService {

    /**
     * Well-known account that owns the bank's fee-collection wallets. Seeded in a
     * migration for reporting; the wallets under it are created lazily on demand.
     */
    public static final UUID FEE_ACCOUNT_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000f0ee5");

    private final WalletRepository wallets;
    private final LedgerService ledger;
    private final AccountRepository accounts;
    private final UserRepository users;

    public DefaultWalletService(WalletRepository wallets, LedgerService ledger,
                                AccountRepository accounts, UserRepository users) {
        this.wallets = wallets;
        this.ledger = ledger;
        this.accounts = accounts;
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletListItem> list() {
        List<Wallet> all = wallets.findAll().stream()
                .filter(w -> !FEE_ACCOUNT_ID.equals(w.getAccountId()))
                .toList();

        Set<UUID> accountIds = all.stream().map(Wallet::getAccountId).collect(Collectors.toSet());
        Map<UUID, UUID> ownerByAccount = accounts.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Account::getOwnerUserId));
        Map<UUID, String> emailByUser = users.findAllById(ownerByAccount.values()).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));

        return all.stream()
                .map(w -> new WalletListItem(
                        w.getId(),
                        w.getAccountId(),
                        emailByUser.getOrDefault(ownerByAccount.get(w.getAccountId()), "unknown"),
                        w.getCurrency(),
                        w.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyWalletView> listMine(UUID ownerUserId) {
        Set<UUID> accountIds = accounts.findAllByOwnerUserId(ownerUserId).stream()
                .map(Account::getId)
                .collect(Collectors.toSet());
        if (accountIds.isEmpty()) return List.of();

        return wallets.findByAccountIdIn(accountIds).stream()
                .map(w -> new MyWalletView(
                        w.getId(),
                        w.getAccountId(),
                        w.getCurrency(),
                        w.getStatus(),
                        ledger.balanceOf(w.getId())))
                .toList();
    }

    @Override
    @Transactional
    public UUID systemFeeWalletId(String currency) {
        String ccy = currency.toUpperCase();
        return wallets.findByAccountIdAndCurrency(FEE_ACCOUNT_ID, ccy)
                .map(Wallet::getId)
                .orElseGet(() -> wallets.save(new Wallet(FEE_ACCOUNT_ID, ccy)).getId());
    }

    @Override
    @Transactional
    public WalletView open(UUID accountId, String currency) {
        return WalletView.of(wallets.save(new Wallet(accountId, currency.toUpperCase())));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal balance(UUID walletId) {
        require(walletId);
        return ledger.balanceOf(walletId);   // derived from immutable ledger facts
    }

    @Override
    @Transactional
    public WalletView freeze(UUID walletId) {
        Wallet w = require(walletId);
        w.setStatus(WalletStatus.FROZEN);
        return WalletView.of(w);
    }

    @Override
    @Transactional
    public WalletView unfreeze(UUID walletId) {
        Wallet w = require(walletId);
        if (w.getStatus() == WalletStatus.CLOSED) {
            throw new ApiException("WALLET_CLOSED", "Closed wallet cannot be reactivated", 409);
        }
        w.setStatus(WalletStatus.ACTIVE);
        return WalletView.of(w);
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getActive(UUID walletId) {
        Wallet w = require(walletId);
        if (w.getStatus() != WalletStatus.ACTIVE) {
            throw new ApiException("WALLET_NOT_ACTIVE",
                    "Wallet is " + w.getStatus(), 422);
        }
        return w;
    }

    private Wallet require(UUID id) {
        return wallets.findById(id)
                .orElseThrow(() -> new ApiException("WALLET_NOT_FOUND", "Unknown wallet", 404));
    }
}
