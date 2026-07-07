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
    public ReportContent download(UUID reportId, UUID requestedBy) {
        ReportRecord rec = reports.findById(reportId)
                .orElseThrow(() -> new ApiException("REPORT_NOT_FOUND", "Report not found", 404));
        if (!"READY".equals(rec.getStatus())) {
            throw new ApiException("REPORT_NOT_READY", "Report is not yet ready", 202);
        }
        // FR-24.2: render in the requested format (CSV or Excel/SpreadsheetML).
        String[][] rows = {
                {"reportId", "type", "from", "to"},
                {rec.getId().toString(), rec.getReportType(),
                        rec.getFromDate().toString(), rec.getToDate().toString()}
        };
        if (isExcel(rec.getFormat())) {
            byte[] xls = toSpreadsheetMl(rows).getBytes(StandardCharsets.UTF_8);
            return new ReportContent(xls, "application/vnd.ms-excel", "report-" + reportId + ".xls");
        }
        return new ReportContent(toCsv(rows).getBytes(StandardCharsets.UTF_8),
                "text/csv", "report-" + reportId + ".csv");
    }

    private static boolean isExcel(String format) {
        return format != null
                && (format.equalsIgnoreCase("EXCEL") || format.equalsIgnoreCase("XLSX")
                    || format.equalsIgnoreCase("XLS"));
    }

    private static String toCsv(String[][] rows) {
        StringBuilder sb = new StringBuilder();
        for (String[] row : rows) {
            sb.append(String.join(",", row)).append('\n');
        }
        return sb.toString();
    }

    /** Minimal SpreadsheetML 2003 — a dependency-free workbook Excel opens natively. */
    private static String toSpreadsheetMl(String[][] rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n")
          .append("<?mso-application progid=\"Excel.Sheet\"?>\n")
          .append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ")
          .append("xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n")
          .append("<Worksheet ss:Name=\"Report\"><Table>\n");
        for (String[] row : rows) {
            sb.append("<Row>");
            for (String cell : row) {
                sb.append("<Cell><Data ss:Type=\"String\">").append(xml(cell)).append("</Data></Cell>");
            }
            sb.append("</Row>\n");
        }
        sb.append("</Table></Worksheet></Workbook>");
        return sb.toString();
    }

    private static String xml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
