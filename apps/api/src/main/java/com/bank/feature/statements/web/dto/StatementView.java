package com.bank.feature.statements.web.dto;

import com.bank.feature.statements.persistence.StatementRecord;

import java.time.Instant;
import java.util.UUID;

public record StatementView(
        UUID id,
        UUID accountId,
        int periodYear,
        int periodMonth,
        String status,
        Instant createdAt
) {
    public static StatementView of(StatementRecord r) {
        return new StatementView(r.getId(), r.getAccountId(),
                r.getPeriodYear(), r.getPeriodMonth(), r.getStatus(), r.getCreatedAt());
    }
}
