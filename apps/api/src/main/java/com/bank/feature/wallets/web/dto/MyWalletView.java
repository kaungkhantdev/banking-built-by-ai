package com.bank.feature.wallets.web.dto;

import com.bank.feature.wallets.persistence.WalletStatus;

import java.math.BigDecimal;
import java.util.UUID;

/** A wallet owned by the signed-in customer, with its derived balance. */
public record MyWalletView(UUID id, UUID accountId, String currency, WalletStatus status, BigDecimal balance) {
}
