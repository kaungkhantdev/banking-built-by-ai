package com.bank.feature.notifications.domain;

import com.bank.feature.notifications.persistence.NotificationLog;
import com.bank.feature.notifications.persistence.NotificationLogRepository;
import com.bank.feature.notifications.persistence.NotificationTemplate;
import com.bank.feature.notifications.persistence.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class DefaultNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final NotificationLogRepository logs;
    private final NotificationTemplateRepository templates;

    public DefaultNotificationService(NotificationLogRepository logs,
                                      NotificationTemplateRepository templates) {
        this.logs = logs;
        this.templates = templates;
    }

    @Override
    @Transactional
    public void send(UUID eventId, String channel, String recipient, String template, Map<String, Object> vars) {
        if (logs.existsByEventIdAndChannel(eventId, channel)) {
            log.debug("Notification already sent for event={} channel={}", eventId, channel);
            return;
        }

        // FR-13.5: resolve a configurable template (fall back to the raw code).
        String rendered = templates.findById(template)
                .map(t -> render(t, vars))
                .orElse("[" + template + "] " + vars);

        String status = deliverWithRetry(eventId, channel, recipient, rendered);
        logs.save(new NotificationLog(eventId, channel, recipient, template, status));
    }

    /** FR-13.4: retry transient delivery failures before giving up. */
    private String deliverWithRetry(UUID eventId, String channel, String recipient, String rendered) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                deliver(channel, recipient, rendered);
                return "SENT";
            } catch (Exception e) {
                log.warn("Notification delivery attempt {}/{} failed for event={}: {}",
                        attempt, MAX_ATTEMPTS, eventId, e.getMessage());
            }
        }
        return "FAILED";
    }

    /** Stub transport: log to console instead of a real email/SMS gateway. */
    private void deliver(String channel, String recipient, String rendered) {
        log.info("[NOTIFICATION] channel={} to={} :: {}", channel, recipient, rendered);
    }

    private String render(NotificationTemplate t, Map<String, Object> vars) {
        String body = t.getBody();
        if (vars != null) {
            for (Map.Entry<String, Object> e : vars.entrySet()) {
                body = body.replace("{{" + e.getKey() + "}}", String.valueOf(e.getValue()));
            }
        }
        return t.getSubject() + " — " + body;
    }
}
