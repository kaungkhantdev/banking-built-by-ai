package com.bank.feature.scheduler.domain;

import com.bank.feature.scheduler.persistence.ScheduledTransfer;
import com.bank.feature.scheduler.persistence.ScheduledTransferRepository;
import com.bank.feature.scheduler.web.dto.ScheduledTransferView;
import com.bank.feature.transfers.domain.TransferCommand;
import com.bank.feature.transfers.domain.TransferService;
import com.bank.shared.exception.ApiException;
import com.bank.shared.utils.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultSchedulerService implements SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(DefaultSchedulerService.class);

    private final ScheduledTransferRepository repo;
    private final TransferService transfers;

    public DefaultSchedulerService(ScheduledTransferRepository repo, TransferService transfers) {
        this.repo = repo;
        this.transfers = transfers;
    }

    @Override
    @Transactional
    public ScheduledTransferView schedule(UUID ownerUserId, UUID fromWalletId, UUID toWalletId,
                                           BigDecimal amount, String memo,
                                           String recurrenceRule, Instant runAt) {
        if (!Money.isPositive(amount)) {
            throw new ApiException("AMOUNT_INVALID", "Amount must be positive", 422);
        }
        ScheduledTransfer s = repo.save(new ScheduledTransfer(
                ownerUserId, fromWalletId, toWalletId, amount, memo, recurrenceRule, runAt));
        return ScheduledTransferView.of(s);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScheduledTransferView> list(UUID ownerUserId, Pageable pageable) {
        return repo.findByOwnerUserId(ownerUserId, pageable).map(ScheduledTransferView::of);
    }

    @Override
    @Transactional
    public void cancel(UUID id, UUID ownerUserId) {
        ScheduledTransfer s = repo.findById(id)
                .orElseThrow(() -> new ApiException("SCHEDULED_TRANSFER_NOT_FOUND", "Not found", 404));
        if (!s.getOwnerUserId().equals(ownerUserId)) {
            throw new ApiException("FORBIDDEN", "You do not own this scheduled transfer", 403);
        }
        s.setStatus("CANCELLED");
    }

    @Scheduled(fixedDelay = 30_000)
    public void runDueTransfers() {
        List<ScheduledTransfer> due = repo.findDue(Instant.now());
        for (ScheduledTransfer s : due) {
            try {
                UUID txId = UUID.randomUUID();
                TransferCommand cmd = new TransferCommand(
                        txId, s.getFromWalletId(), s.getToWalletId(),
                        s.getAmount(), "sched:" + s.getId() + ":" + s.getAttemptCount(), s.getMemo());
                transfers.transfer(cmd);
                s.setLastRunAt(Instant.now());
                s.setLastError(null);
                advanceNextRun(s);
                repo.save(s);
            } catch (Exception e) {
                s.incrementAttemptCount();
                s.setLastError(e.getMessage());
                if (s.getAttemptCount() >= s.getMaxAttempts()) {
                    s.setStatus("FAILED");
                }
                repo.save(s);
                log.warn("[SCHEDULER] Failed scheduled transfer {}: {}", s.getId(), e.getMessage());
            }
        }
    }

    private void advanceNextRun(ScheduledTransfer s) {
        if (s.getRecurrenceRule() == null) {
            s.setStatus("CANCELLED"); // one-off
            return;
        }
        switch (s.getRecurrenceRule()) {
            case "DAILY"   -> s.setNextRunAt(s.getNextRunAt().plus(1, ChronoUnit.DAYS));
            case "WEEKLY"  -> s.setNextRunAt(s.getNextRunAt().plus(7, ChronoUnit.DAYS));
            case "MONTHLY" -> s.setNextRunAt(s.getNextRunAt().plus(30, ChronoUnit.DAYS));
            default -> s.setStatus("CANCELLED");
        }
    }
}
