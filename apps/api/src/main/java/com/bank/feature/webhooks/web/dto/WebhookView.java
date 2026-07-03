package com.bank.feature.webhooks.web.dto;

import com.bank.feature.webhooks.persistence.WebhookEndpoint;

import java.time.Instant;
import java.util.UUID;

public record WebhookView(UUID id, UUID ownerUserId, String url, String eventTypes,
                           boolean active, Instant createdAt) {
    public static WebhookView of(WebhookEndpoint e) {
        return new WebhookView(e.getId(), e.getOwnerUserId(), e.getUrl(),
                e.getEventTypes(), e.isActive(), e.getCreatedAt());
    }
}
