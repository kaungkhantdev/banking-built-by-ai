package com.bank.feature.exchange.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 3)
    private String fromCurrency;

    @Column(nullable = false, length = 3)
    private String toCurrency;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(nullable = false)
    private Instant effectiveAt;

    @Column
    private String source;

    @Column(nullable = false)
    private Instant createdAt;

    protected ExchangeRate() {}

    public ExchangeRate(String fromCurrency, String toCurrency,
                         BigDecimal rate, Instant effectiveAt, String source) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.effectiveAt = effectiveAt;
        this.source = source;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getFromCurrency() { return fromCurrency; }
    public String getToCurrency() { return toCurrency; }
    public BigDecimal getRate() { return rate; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public String getSource() { return source; }
}
