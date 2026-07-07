package com.bank.feature.accounts.domain;

import com.bank.feature.accounts.web.dto.AccountListItem;
import com.bank.feature.accounts.web.dto.AccountOverview;
import com.bank.feature.accounts.web.dto.AccountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Port: account lifecycle. */
public interface AccountService {

    AccountView open(UUID ownerUserId);

    /** PENDING -> ACTIVE, typically after KYC verification. */
    AccountView activate(UUID accountId);

    AccountView freeze(UUID accountId);

    AccountView get(UUID accountId);

    Page<AccountListItem> list(Pageable pageable);

    /** Accounts owned by a single user (customer-scoped view). */
    List<AccountListItem> listForUser(UUID ownerUserId);

    /**
     * Full detail for one account: holder/KYC status, wallets with balances and
     * recent transactions. Access is allowed to the owner, or to a caller with
     * {@code account:manage} ({@code isManager}); anyone else gets 403.
     */
    AccountOverview overview(UUID accountId, UUID requesterUserId, boolean isManager);
}
