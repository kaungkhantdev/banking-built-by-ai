package com.bank.feature.accounts.web.dto;

import com.bank.feature.accounts.persistence.Account;
import com.bank.feature.accounts.persistence.AccountStatus;

import java.util.UUID;

public record AccountView(UUID id, UUID ownerUserId, AccountStatus status) {

    public static AccountView of(Account a) {
        return new AccountView(a.getId(), a.getOwnerUserId(), a.getStatus());
    }
}
