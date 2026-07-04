package com.bank.feature.webhooks.web.dto;

import com.bank.feature.webhooks.persistence.WebhookDelivery;

import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryView(
        UUID id,
        UUID endpointId,
        String eventType,
        String status,
        int attempts,
        Integer responseCode,
        String lastError,
        Instant nextAttemptAt,
        Instant createdAt) {

    public static WebhookDeliveryView of(WebhookDelivery d) {
        return new WebhookDeliveryView(d.getId(), d.getEndpointId(), d.getEventType(),
                d.getStatus(), d.getAttempts(), d.getResponseCode(), d.getLastError(),
                d.getNextAttemptAt(), d.getCreatedAt());
    }
}
