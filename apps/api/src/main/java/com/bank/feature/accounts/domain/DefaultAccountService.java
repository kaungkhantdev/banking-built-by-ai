package com.bank.feature.accounts.domain;

import com.bank.feature.accounts.persistence.Account;
import com.bank.feature.accounts.persistence.AccountRepository;
import com.bank.feature.accounts.persistence.AccountStatus;
import com.bank.feature.accounts.web.dto.AccountListItem;
import com.bank.feature.accounts.web.dto.AccountOverview;
import com.bank.feature.accounts.web.dto.AccountView;
import com.bank.feature.auth.persistence.User;
import com.bank.feature.auth.persistence.UserRepository;
import com.bank.feature.history.domain.HistoryService;
import com.bank.feature.kyc.domain.KycService;
import com.bank.feature.ledger.domain.LedgerService;
import com.bank.feature.wallets.persistence.Wallet;
import com.bank.feature.wallets.persistence.WalletRepository;
import com.bank.shared.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DefaultAccountService implements AccountService {

    private final AccountRepository accounts;
    private final UserRepository users;
    private final WalletRepository wallets;
    private final LedgerService ledger;
    private final KycService kyc;
    private final HistoryService history;

    public DefaultAccountService(AccountRepository accounts, UserRepository users,
                                 WalletRepository wallets, LedgerService ledger,
                                 KycService kyc, HistoryService history) {
        this.accounts = accounts;
        this.users = users;
        this.wallets = wallets;
        this.ledger = ledger;
        this.kyc = kyc;
        this.history = history;
    }

    @Override
    @Transactional
    public AccountView open(UUID ownerUserId) {
        return AccountView.of(accounts.save(new Account(ownerUserId)));
    }

    @Override
    @Transactional
    public AccountView activate(UUID accountId) {
        Account a = require(accountId);
        if (a.getStatus() != AccountStatus.PENDING) {
            throw new ApiException("ACCOUNT_ILLEGAL_TRANSITION",
                    "Only PENDING accounts can be activated (was " + a.getStatus() + ")", 409);
        }
        a.setStatus(AccountStatus.ACTIVE);
        return AccountView.of(a);
    }

    @Override
    @Transactional
    public AccountView freeze(UUID accountId) {
        Account a = require(accountId);
        a.setStatus(AccountStatus.FROZEN);
        return AccountView.of(a);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountView get(UUID accountId) {
        return AccountView.of(require(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountListItem> list(Pageable pageable) {
        Page<Account> page = accounts.findAll(pageable);
        Set<UUID> ownerIds = page.stream().map(Account::getOwnerUserId).collect(Collectors.toSet());
        Map<UUID, String> emailByUserId = users.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
        return page.map(a -> new AccountListItem(
                a.getId(),
                a.getOwnerUserId(),
                emailByUserId.getOrDefault(a.getOwnerUserId(), "unknown"),
                a.getStatus(),
                a.getCreatedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountListItem> listForUser(UUID ownerUserId) {
        String email = users.findById(ownerUserId).map(User::getEmail).orElse("unknown");
        return accounts.findAllByOwnerUserId(ownerUserId).stream()
                .map(a -> new AccountListItem(
                        a.getId(),
                        a.getOwnerUserId(),
                        email,
                        a.getStatus(),
                        a.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountOverview overview(UUID accountId, UUID requesterUserId, boolean isManager) {
        Account account = require(accountId);
        if (!isManager && !account.getOwnerUserId().equals(requesterUserId)) {
            throw new ApiException("FORBIDDEN", "This account belongs to another user", 403);
        }

        String email = users.findById(account.getOwnerUserId()).map(User::getEmail).orElse("unknown");
        String kycStatus = kyc.statusOf(accountId).name();

        List<Wallet> accountWallets = wallets.findByAccountId(accountId);
        List<AccountOverview.WalletBrief> walletBriefs = accountWallets.stream()
                .map(w -> new AccountOverview.WalletBrief(
                        w.getId(), w.getCurrency(), w.getStatus().name(), ledger.balanceOf(w.getId())))
                .toList();

        // Pull the newest few rows per wallet, then merge into a single recent feed.
        Pageable recent = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "postedAt"));
        List<AccountOverview.TxBrief> recentTransactions = accountWallets.stream()
                .flatMap(w -> history.listForWallet(w.getId(), null, null, null, null, recent)
                        .getContent().stream())
                .map(t -> new AccountOverview.TxBrief(
                        t.id(), t.walletId(), t.currency(), t.direction(),
                        t.amount(), t.memo(), t.postedAt()))
                .sorted(Comparator.comparing(AccountOverview.TxBrief::postedAt).reversed())
                .limit(10)
                .toList();

        return new AccountOverview(
                account.getId(),
                account.getOwnerUserId(),
                email,
                account.getStatus().name(),
                account.getCreatedAt(),
                kycStatus,
                walletBriefs,
                recentTransactions);
    }

    private Account require(UUID id) {
        return accounts.findById(id)
                .orElseThrow(() -> new ApiException("ACCOUNT_NOT_FOUND", "Unknown account", 404));
    }
}
