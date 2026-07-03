package com.bank.feature.notifications.domain;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {

    /** Deliver a notification, deduplicating on (eventId, channel). */
    void send(UUID eventId, String channel, String recipient, String template, Map<String, Object> vars);
}
