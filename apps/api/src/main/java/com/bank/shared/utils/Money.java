package com.bank.shared.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money helper. All amounts are {@link BigDecimal} with a fixed scale of 4 —
 * binary floating point cannot represent decimal money exactly
 * ({@code 0.1 + 0.2 != 0.3}), which is unacceptable in a ledger.
 *
 * <p>The ledger column is {@code NUMERIC(19,4)}; this utility keeps application
 * values aligned to the same scale and provides the only sanctioned rounding
 * mode (HALF_EVEN, "banker's rounding").
 */
public final class Money {

    public static final int SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private Money() {
    }

    /** Normalize any BigDecimal to the canonical money scale. */
    public static BigDecimal of(BigDecimal raw) {
        return raw.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal of(String raw) {
        return of(new BigDecimal(raw));
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /** True when {@code balance} can cover {@code amount} (both at money scale). */
    public static boolean covers(BigDecimal balance, BigDecimal amount) {
        return of(balance).compareTo(of(amount)) >= 0;
    }
}
