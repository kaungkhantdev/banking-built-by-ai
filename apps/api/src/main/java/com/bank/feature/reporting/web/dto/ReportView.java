package com.bank.feature.reporting.web.dto;

import com.bank.feature.reporting.persistence.ReportRecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReportView(
        UUID id,
        String reportType,
        LocalDate fromDate,
        LocalDate toDate,
        String format,
        String status,
        Instant createdAt
) {
    public static ReportView of(ReportRecord r) {
        return new ReportView(r.getId(), r.getReportType(), r.getFromDate(),
                r.getToDate(), r.getFormat(), r.getStatus(), r.getCreatedAt());
    }
}
