package com.bank.feature.scheduler.domain;

import com.bank.feature.scheduler.web.dto.ScheduledTransferView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface SchedulerService {

    ScheduledTransferView schedule(UUID ownerUserId, UUID fromWalletId, UUID toWalletId,
                                   BigDecimal amount, String memo, String recurrenceRule, Instant runAt);

    Page<ScheduledTransferView> list(UUID ownerUserId, Pageable pageable);

    void cancel(UUID id, UUID ownerUserId);
}
