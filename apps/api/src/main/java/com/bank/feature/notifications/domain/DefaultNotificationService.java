package com.bank.feature.notifications.domain;

import com.bank.feature.notifications.persistence.NotificationLog;
import com.bank.feature.notifications.persistence.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class DefaultNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);

    private final NotificationLogRepository logs;

    public DefaultNotificationService(NotificationLogRepository logs) {
        this.logs = logs;
    }

    @Override
    @Transactional
    public void send(UUID eventId, String channel, String recipient, String template, Map<String, Object> vars) {
        if (logs.existsByEventIdAndChannel(eventId, channel)) {
            log.debug("Notification already sent for event={} channel={}", eventId, channel);
            return;
        }
        String status = "SENT";
        try {
            // Stub: log to console instead of a real transport
            log.info("[NOTIFICATION] channel={} to={} template={} vars={}", channel, recipient, template, vars);
        } catch (Exception e) {
            log.warn("Notification delivery failed for event={}: {}", eventId, e.getMessage());
            status = "FAILED";
        }
        logs.save(new NotificationLog(eventId, channel, recipient, template, status));
    }
}
