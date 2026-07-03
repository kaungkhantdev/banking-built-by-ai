package com.bank.feature.events.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    // Oldest still-unpublished event — used to compute outbox relay lag for health.
    Optional<OutboxEvent> findFirstByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
