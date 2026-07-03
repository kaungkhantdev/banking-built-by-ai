package com.bank.feature.scheduler.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, UUID> {

    Page<ScheduledTransfer> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

    @Query("select s from ScheduledTransfer s where s.status = 'ACTIVE' and s.nextRunAt <= :now")
    List<ScheduledTransfer> findDue(@Param("now") Instant now);
}
