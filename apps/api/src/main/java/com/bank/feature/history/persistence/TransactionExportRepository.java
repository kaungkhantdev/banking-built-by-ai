package com.bank.feature.history.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionExportRepository extends JpaRepository<TransactionExport, UUID> {
}
