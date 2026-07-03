package com.bank.feature.reporting.domain;

import com.bank.feature.reporting.persistence.ReportRecord;
import com.bank.feature.reporting.persistence.ReportRecordRepository;
import com.bank.feature.reporting.web.dto.ReportView;
import com.bank.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultReportService implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(DefaultReportService.class);

    private final ReportRecordRepository reports;

    public DefaultReportService(ReportRecordRepository reports) {
        this.reports = reports;
    }

    @Override
    @Transactional
    public ReportView request(String reportType, LocalDate from, LocalDate to,
                               String format, UUID requestedBy) {
        if (from.isAfter(to)) {
            throw new ApiException("REPORT_PERIOD_INVALID", "from must be before to", 422);
        }
        ReportRecord rec = reports.save(
                new ReportRecord(reportType, from, to, format, null, requestedBy));
        generateAsync(rec.getId());
        return ReportView.of(rec);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportView> list(UUID requestedBy) {
        return reports.findByRequestedByOrderByCreatedAtDesc(requestedBy)
                .stream().map(ReportView::of).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] download(UUID reportId, UUID requestedBy) {
        ReportRecord rec = reports.findById(reportId)
                .orElseThrow(() -> new ApiException("REPORT_NOT_FOUND", "Report not found", 404));
        if (!"READY".equals(rec.getStatus())) {
            throw new ApiException("REPORT_NOT_READY", "Report is not yet ready", 202);
        }
        // Stub: return CSV-like placeholder
        String csv = "reportId,type,from,to\n" + rec.getId() + "," + rec.getReportType()
                + "," + rec.getFromDate() + "," + rec.getToDate();
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    @Async
    void generateAsync(UUID reportId) {
        reports.findById(reportId).ifPresent(rec -> {
            try {
                rec.setStatus("READY");
                rec.setFileKey("reports/" + reportId + "." + rec.getFormat().toLowerCase());
                reports.save(rec);
                log.info("[REPORT] Generated {}", reportId);
            } catch (Exception e) {
                reports.findById(reportId).ifPresent(r -> {
                    r.setStatus("FAILED");
                    reports.save(r);
                });
            }
        });
    }
}
