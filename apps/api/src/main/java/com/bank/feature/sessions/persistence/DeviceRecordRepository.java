package com.bank.feature.sessions.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeviceRecordRepository extends JpaRepository<DeviceRecord, UUID> {

    List<DeviceRecord> findByUserIdOrderByLoggedInAtDesc(UUID userId);
}
