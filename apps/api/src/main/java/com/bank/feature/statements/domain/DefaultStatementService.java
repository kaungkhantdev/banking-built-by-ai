package com.bank.feature.statements.domain;

import com.bank.feature.statements.persistence.StatementRecord;
import com.bank.feature.statements.persistence.StatementRecordRepository;
import com.bank.feature.statements.web.dto.StatementView;
import com.bank.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultStatementService implements StatementService {

    private static final Logger log = LoggerFactory.getLogger(DefaultStatementService.class);

    private final StatementRecordRepository statements;

    public DefaultStatementService(StatementRecordRepository statements) {
        this.statements = statements;
    }

    @Override
    @Transactional
    public StatementView request(UUID accountId, int year, int month) {
        YearMonth requested = YearMonth.of(year, month);
        if (!requested.isBefore(YearMonth.now())) {
            throw new ApiException("STATEMENT_PERIOD_INVALID",
                    "Statements cannot be requested for the current or a future month", 422);
        }
        return statements.findByAccountIdAndPeriodYearAndPeriodMonth(accountId, year, month)
                .map(StatementView::of)
                .orElseGet(() -> {
                    StatementRecord rec = statements.save(new StatementRecord(accountId, year, month));
                    generateAsync(rec.getId());
                    return StatementView.of(rec);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementView> list(UUID accountId) {
        return statements.findByAccountIdOrderByPeriodYearDescPeriodMonthDesc(accountId)
                .stream().map(StatementView::of).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] download(UUID accountId, UUID statementId) {
        StatementRecord rec = statements.findById(statementId)
                .orElseThrow(() -> new ApiException("STATEMENT_NOT_FOUND", "Statement not found", 404));
        if (!rec.getAccountId().equals(accountId)) {
            throw new ApiException("FORBIDDEN", "Statement does not belong to this account", 403);
        }
        if (!"READY".equals(rec.getStatus())) {
            throw new ApiException("STATEMENT_NOT_READY", "Statement is not yet ready", 202);
        }
        // Stub: return a placeholder PDF-like byte array
        String stub = "Statement for account " + accountId + " period "
                + rec.getPeriodYear() + "-" + String.format("%02d", rec.getPeriodMonth());
        return stub.getBytes(StandardCharsets.UTF_8);
    }

    @Async
    void generateAsync(UUID statementId) {
        statements.findById(statementId).ifPresent(rec -> {
            try {
                // Stub: mark READY immediately
                rec.setStatus("READY");
                rec.setFileKey("statements/" + statementId + ".pdf");
                statements.save(rec);
                log.info("[STATEMENT] Generated {}", statementId);
            } catch (Exception e) {
                statements.findById(statementId).ifPresent(r -> {
                    r.setStatus("FAILED");
                    statements.save(r);
                });
            }
        });
    }
}
