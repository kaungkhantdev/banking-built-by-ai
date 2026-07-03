package com.bank.feature.wallets.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceView(UUID walletId, BigDecimal balance) {
}
