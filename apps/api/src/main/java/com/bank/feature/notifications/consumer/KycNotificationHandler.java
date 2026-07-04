package com.bank.feature.notifications.consumer;

import com.bank.config.RabbitTopologyConfig;
import com.bank.feature.notifications.domain.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** FR-13.2: notify the customer when their KYC status changes. */
@Component
public class KycNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(KycNotificationHandler.class);

    private final NotificationService notifications;

    public KycNotificationHandler(NotificationService notifications) {
        this.notifications = notifications;
    }

    @RabbitListener(queues = RabbitTopologyConfig.KYC_QUEUE, ackMode = "AUTO")
    public void onKycStatusChanged(Map<String, Object> payload) {
        try {
            UUID eventId = eventId(payload);
            notifications.send(eventId, "EMAIL", "customer", "kyc_status_changed", payload);
        } catch (Exception e) {
            log.warn("Failed to process KYC notification: {}", e.getMessage());
            throw e;
        }
    }

    private static UUID eventId(Map<String, Object> payload) {
        Object id = payload.get("eventId");
        return id != null ? UUID.fromString(id.toString()) : UUID.randomUUID();
    }
}
