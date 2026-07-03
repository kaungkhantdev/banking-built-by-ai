package com.bank.shared.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void normalizesToFourDecimalPlaces() {
        assertThat(Money.of("10").toPlainString()).isEqualTo("10.0000");
        assertThat(Money.of(new BigDecimal("1.23456")).toPlainString()).isEqualTo("1.2346"); // HALF_EVEN
    }

    @Test
    void coversIsInclusiveAtEquality() {
        assertThat(Money.covers(new BigDecimal("25.00"), new BigDecimal("25"))).isTrue();
        assertThat(Money.covers(new BigDecimal("24.9999"), new BigDecimal("25"))).isFalse();
    }

    @Test
    void positiveCheck() {
        assertThat(Money.isPositive(new BigDecimal("0.0001"))).isTrue();
        assertThat(Money.isPositive(BigDecimal.ZERO)).isFalse();
        assertThat(Money.isPositive(new BigDecimal("-1"))).isFalse();
    }
}
