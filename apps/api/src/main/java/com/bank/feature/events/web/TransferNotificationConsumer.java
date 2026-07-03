package com.bank.feature.events.web;

import com.bank.config.RabbitTopologyConfig;
import com.bank.feature.events.persistence.ProcessedEvent;
import com.bank.feature.events.persistence.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Idempotent consumer for {@code transfer.completed}. Dedups on the event id
 * (insert-first into {@code processed_events}); a duplicate is acked and skipped.
 * Demonstrates the at-least-once → effectively-once pattern.
 */
@Component
public class TransferNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferNotificationConsumer.class);

    private final ProcessedEventRepository processed;

    public TransferNotificationConsumer(ProcessedEventRepository processed) {
        this.processed = processed;
    }

    @RabbitListener(queues = RabbitTopologyConfig.TRANSFER_QUEUE)
    @Transactional
    public void onTransferCompleted(Message msg) {
        String idHeader = msg.getMessageProperties().getMessageId();
        if (idHeader == null) {
            return;   // unkeyed message — cannot dedup; drop to DLX via no-op
        }
        UUID eventId = UUID.fromString(idHeader);

        if (processed.existsById(eventId)) {
            return;   // already handled
        }
        try {
            processed.saveAndFlush(new ProcessedEvent(eventId, Instant.now()));
        } catch (DataIntegrityViolationException duplicate) {
            return;   // concurrent duplicate
        }

        String body = new String(msg.getBody(), StandardCharsets.UTF_8);
        log.info("Sending transfer receipt notification: {}", body);
        // real impl: hand off to an email/push sender
    }
}
