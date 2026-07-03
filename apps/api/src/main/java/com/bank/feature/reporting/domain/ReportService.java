package com.bank.feature.reporting.domain;

import com.bank.feature.reporting.web.dto.ReportView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportService {

    ReportView request(String reportType, LocalDate from, LocalDate to, String format, UUID requestedBy);

    List<ReportView> list(UUID requestedBy);

    byte[] download(UUID reportId, UUID requestedBy);
}
