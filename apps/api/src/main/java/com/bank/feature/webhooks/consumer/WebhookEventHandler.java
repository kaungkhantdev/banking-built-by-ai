package com.bank.feature.webhooks.consumer;

import com.bank.config.RabbitTopologyConfig;
import com.bank.feature.webhooks.domain.WebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * FR-26.1/26.4: fans every domain event out to subscribed webhook endpoints by
 * enqueuing a delivery row per endpoint. The {@link com.bank.feature.webhooks.domain.WebhookDispatcher}
 * then signs and delivers them with retry.
 */
@Component
public class WebhookEventHandler {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventHandler.class);

    private final WebhookService webhooks;
    private final ObjectMapper objectMapper;

    public WebhookEventHandler(WebhookService webhooks, ObjectMapper objectMapper) {
        this.webhooks = webhooks;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitTopologyConfig.WEBHOOK_QUEUE, ackMode = "AUTO")
    public void onEvent(Map<String, Object> payload,
                        @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String eventType) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            int enqueued = webhooks.enqueue(eventType, json);
            if (enqueued > 0) {
                log.debug("[WEBHOOK] enqueued {} deliveries for {}", enqueued, eventType);
            }
        } catch (Exception e) {
            log.warn("Failed to enqueue webhook deliveries for {}: {}", eventType, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
