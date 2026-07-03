package com.bank.feature.notifications.consumer;

import com.bank.config.RabbitTopologyConfig;
import com.bank.feature.notifications.domain.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** Consumes transfer.completed events and dispatches email notifications. */
@Component
public class TransferNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(TransferNotificationHandler.class);

    private final NotificationService notifications;

    public TransferNotificationHandler(NotificationService notifications) {
        this.notifications = notifications;
    }

    @RabbitListener(queues = RabbitTopologyConfig.TRANSFER_QUEUE, ackMode = "AUTO")
    public void onTransferCompleted(Map<String, Object> payload) {
        try {
            UUID eventId = UUID.randomUUID(); // deduplicate downstream by content if needed
            notifications.send(eventId, "EMAIL", "customer", "transfer_completed", payload);
        } catch (Exception e) {
            log.warn("Failed to process transfer notification: {}", e.getMessage());
            throw e; // re-throw so RabbitMQ can retry / DLQ
        }
    }
}
