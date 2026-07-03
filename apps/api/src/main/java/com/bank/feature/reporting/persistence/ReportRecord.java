package com.bank.feature.reporting.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "report_records")
public class ReportRecord extends BaseAuditEntity {

    @Column(nullable = false)
    private String reportType;

    @Column(nullable = false)
    private LocalDate fromDate;

    @Column(nullable = false)
    private LocalDate toDate;

    @Column(nullable = false)
    private String format;

    @Column(nullable = false)
    private String status;

    @Column
    private String fileKey;

    @Column(columnDefinition = "TEXT")
    private String filters;

    @Column(nullable = false)
    private UUID requestedBy;

    protected ReportRecord() {}

    public ReportRecord(String reportType, LocalDate fromDate, LocalDate toDate,
                         String format, String filters, UUID requestedBy) {
        this.reportType = reportType;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.format = format;
        this.filters = filters;
        this.requestedBy = requestedBy;
        this.status = "PENDING";
    }

    public String getReportType() { return reportType; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
    public String getFormat() { return format; }
    public String getStatus() { return status; }
    public String getFileKey() { return fileKey; }
    public UUID getRequestedBy() { return requestedBy; }

    public void setStatus(String status) { this.status = status; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }
}
