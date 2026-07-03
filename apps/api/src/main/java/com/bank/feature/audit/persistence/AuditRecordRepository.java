package com.bank.feature.audit.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, UUID> {

    @Query("""
            select a from AuditRecord a
            where (:actor is null or a.actor = :actor)
              and (:action is null or a.action = :action)
            order by a.at desc
            """)
    Page<AuditRecord> search(@Param("actor") String actor,
                             @Param("action") String action,
                             Pageable pageable);
}
