package com.bank.feature.reporting.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRecordRepository extends JpaRepository<ReportRecord, UUID> {

    List<ReportRecord> findByRequestedByOrderByCreatedAtDesc(UUID requestedBy);
}
