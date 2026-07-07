package com.bank.feature.wallets.web.dto;

import com.bank.feature.wallets.persistence.WalletStatus;

import java.util.UUID;

/** A wallet enriched with its owner's email for operator pickers. */
public record WalletListItem(UUID id, UUID accountId, String ownerEmail, String currency, WalletStatus status) {
}
