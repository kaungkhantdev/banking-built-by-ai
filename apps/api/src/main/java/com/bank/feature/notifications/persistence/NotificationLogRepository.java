package com.bank.feature.notifications.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    boolean existsByEventIdAndChannel(UUID eventId, String channel);
}
