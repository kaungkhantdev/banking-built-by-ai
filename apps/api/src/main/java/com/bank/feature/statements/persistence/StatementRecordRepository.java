package com.bank.feature.statements.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatementRecordRepository extends JpaRepository<StatementRecord, UUID> {

    List<StatementRecord> findByAccountIdOrderByPeriodYearDescPeriodMonthDesc(UUID accountId);

    Optional<StatementRecord> findByAccountIdAndPeriodYearAndPeriodMonth(
            UUID accountId, int year, int month);
}
