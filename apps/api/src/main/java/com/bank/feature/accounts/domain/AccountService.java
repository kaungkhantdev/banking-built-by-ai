package com.bank.feature.accounts.domain;

import com.bank.feature.accounts.web.dto.AccountListItem;
import com.bank.feature.accounts.web.dto.AccountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Port: account lifecycle. */
public interface AccountService {

    AccountView open(UUID ownerUserId);

    /** PENDING -> ACTIVE, typically after KYC verification. */
    AccountView activate(UUID accountId);

    AccountView freeze(UUID accountId);

    AccountView get(UUID accountId);

    Page<AccountListItem> list(Pageable pageable);
}
