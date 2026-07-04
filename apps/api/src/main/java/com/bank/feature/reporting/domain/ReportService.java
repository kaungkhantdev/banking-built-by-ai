package com.bank.feature.reporting.domain;

import com.bank.feature.reporting.web.dto.ReportView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportService {

    ReportView request(String reportType, LocalDate from, LocalDate to, String format, UUID requestedBy);

    List<ReportView> list(UUID requestedBy);

    ReportContent download(UUID reportId, UUID requestedBy);

    /** A rendered report artifact ready to stream back to the client. */
    record ReportContent(byte[] data, String contentType, String filename) {}
}
