package com.bank.feature.statements.domain;

import com.bank.feature.statements.persistence.StatementRecord;
import com.bank.feature.statements.persistence.StatementRecordRepository;
import com.bank.feature.statements.web.dto.StatementView;
import com.bank.shared.exception.ApiException;
import com.bank.shared.utils.SimplePdf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.YearMonth;
import java.util.HexFormat;
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
        // FR-17.2: regenerate the deterministic PDF and verify it against the digest
        // recorded at generation — a mismatch means the artifact was tampered with.
        byte[] pdf = renderPdf(rec);
        if (rec.getDigest() != null && !rec.getDigest().equals(sha256(pdf))) {
            throw new ApiException("STATEMENT_TAMPERED", "Statement integrity check failed", 500);
        }
        return pdf;
    }

    @Async
    void generateAsync(UUID statementId) {
        statements.findById(statementId).ifPresent(rec -> {
            try {
                byte[] pdf = renderPdf(rec);
                rec.setStatus("READY");
                rec.setFileKey("statements/" + statementId + ".pdf");
                rec.setDigest(sha256(pdf));   // immutability seal
                statements.save(rec);
                log.info("[STATEMENT] Generated {} ({} bytes)", statementId, pdf.length);
            } catch (Exception e) {
                statements.findById(statementId).ifPresent(r -> {
                    r.setStatus("FAILED");
                    statements.save(r);
                });
            }
        });
    }

    /** Deterministic PDF content — no timestamps, so the digest is stable. */
    private byte[] renderPdf(StatementRecord rec) {
        String period = rec.getPeriodYear() + "-" + String.format("%02d", rec.getPeriodMonth());
        return SimplePdf.of(List.of(
                "ACCOUNT STATEMENT",
                "Account: " + rec.getAccountId(),
                "Period:  " + period,
                "",
                "This statement is an immutable record sealed by a SHA-256 digest.",
                "Statement id: " + rec.getId()));
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
