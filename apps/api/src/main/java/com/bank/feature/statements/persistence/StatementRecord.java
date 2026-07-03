package com.bank.feature.statements.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "statement_records")
public class StatementRecord extends BaseAuditEntity {

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private int periodYear;

    @Column(nullable = false)
    private int periodMonth;

    @Column(nullable = false)
    private String status;

    @Column
    private String fileKey;

    @Column
    private String digest;

    protected StatementRecord() {}

    public StatementRecord(UUID accountId, int periodYear, int periodMonth) {
        this.accountId = accountId;
        this.periodYear = periodYear;
        this.periodMonth = periodMonth;
        this.status = "PENDING";
    }

    public UUID getAccountId() { return accountId; }
    public int getPeriodYear() { return periodYear; }
    public int getPeriodMonth() { return periodMonth; }
    public String getStatus() { return status; }
    public String getFileKey() { return fileKey; }
    public String getDigest() { return digest; }

    public void setStatus(String status) { this.status = status; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }
    public void setDigest(String digest) { this.digest = digest; }
}
