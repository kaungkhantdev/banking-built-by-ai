package com.bank.feature.accounts.domain;

import com.bank.feature.accounts.persistence.Account;
import com.bank.feature.accounts.persistence.AccountRepository;
import com.bank.feature.accounts.persistence.AccountStatus;
import com.bank.feature.accounts.web.dto.AccountListItem;
import com.bank.feature.accounts.web.dto.AccountView;
import com.bank.feature.auth.persistence.User;
import com.bank.feature.auth.persistence.UserRepository;
import com.bank.shared.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DefaultAccountService implements AccountService {

    private final AccountRepository accounts;
    private final UserRepository users;

    public DefaultAccountService(AccountRepository accounts, UserRepository users) {
        this.accounts = accounts;
        this.users = users;
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

    private Account require(UUID id) {
        return accounts.findById(id)
                .orElseThrow(() -> new ApiException("ACCOUNT_NOT_FOUND", "Unknown account", 404));
    }
}
