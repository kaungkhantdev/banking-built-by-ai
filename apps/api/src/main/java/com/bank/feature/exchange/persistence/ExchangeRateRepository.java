package com.bank.feature.exchange.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    @Query("""
            select r from ExchangeRate r
            where r.fromCurrency = :from and r.toCurrency = :to
            order by r.effectiveAt desc
            limit 1
            """)
    Optional<ExchangeRate> findLatest(@Param("from") String from, @Param("to") String to);

    @Query("select distinct r.fromCurrency from ExchangeRate r")
    List<String> findDistinctFromCurrencies();

    @Query("select distinct r.toCurrency from ExchangeRate r")
    List<String> findDistinctToCurrencies();
}
