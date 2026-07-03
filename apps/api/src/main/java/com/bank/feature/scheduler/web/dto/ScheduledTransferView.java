package com.bank.feature.scheduler.web.dto;

import com.bank.feature.scheduler.persistence.ScheduledTransfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ScheduledTransferView(
        UUID id,
        UUID fromWalletId,
        UUID toWalletId,
        BigDecimal amount,
        String memo,
        String recurrenceRule,
        Instant nextRunAt,
        String status
) {
    public static ScheduledTransferView of(ScheduledTransfer s) {
        return new ScheduledTransferView(s.getId(), s.getFromWalletId(), s.getToWalletId(),
                s.getAmount(), s.getMemo(), s.getRecurrenceRule(), s.getNextRunAt(), s.getStatus());
    }
}
