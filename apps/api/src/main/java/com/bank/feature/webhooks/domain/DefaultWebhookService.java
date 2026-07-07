package com.bank.feature.webhooks.domain;

import com.bank.feature.webhooks.persistence.WebhookDelivery;
import com.bank.feature.webhooks.persistence.WebhookDeliveryRepository;
import com.bank.feature.webhooks.persistence.WebhookEndpoint;
import com.bank.feature.webhooks.persistence.WebhookEndpointRepository;
import com.bank.feature.webhooks.web.dto.WebhookDeliveryView;
import com.bank.feature.webhooks.web.dto.WebhookView;
import com.bank.shared.exception.ApiException;
import com.bank.shared.utils.Aes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DefaultWebhookService implements WebhookService {

    private final WebhookEndpointRepository endpoints;
    private final WebhookDeliveryRepository deliveries;
    private final Aes aes;

    public DefaultWebhookService(WebhookEndpointRepository endpoints,
                                 WebhookDeliveryRepository deliveries,
                                 Aes aes) {
        this.endpoints = endpoints;
        this.deliveries = deliveries;
        this.aes = aes;
    }

    @Override
    @Transactional
    public WebhookView register(UUID ownerUserId, String url, String secret, String eventTypes) {
        // FR-26.2: the signing secret is stored encrypted at rest (AES-256-GCM).
        WebhookEndpoint ep = endpoints.save(
                new WebhookEndpoint(ownerUserId, url, aes.encrypt(secret), eventTypes));
        return WebhookView.of(ep);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookView> list(UUID ownerUserId) {
        return endpoints.findByOwnerUserId(ownerUserId).stream().map(WebhookView::of).toList();
    }

    @Override
    @Transactional
    public void deregister(UUID id, UUID ownerUserId) {
        WebhookEndpoint ep = endpoints.findById(id)
                .orElseThrow(() -> new ApiException("WEBHOOK_NOT_FOUND", "Webhook not found", 404));
        if (!ep.getOwnerUserId().equals(ownerUserId)) {
            throw new ApiException("FORBIDDEN", "You do not own this webhook", 403);
        }
        ep.setActive(false);
    }

    @Override
    @Transactional
    public int enqueue(String eventType, String payloadJson) {
        int created = 0;
        for (WebhookEndpoint ep : endpoints.findByActiveTrue()) {
            if (subscribed(ep, eventType)) {
                deliveries.save(new WebhookDelivery(ep.getId(), eventType, payloadJson));
                created++;
            }
        }
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookDeliveryView> deliveryHistory(UUID endpointId, UUID ownerUserId) {
        WebhookEndpoint ep = endpoints.findById(endpointId)
                .orElseThrow(() -> new ApiException("WEBHOOK_NOT_FOUND", "Webhook not found", 404));
        if (!ep.getOwnerUserId().equals(ownerUserId)) {
            throw new ApiException("FORBIDDEN", "You do not own this webhook", 403);
        }
        return deliveries.findByEndpointIdOrderByCreatedAtDesc(endpointId)
                .stream().map(WebhookDeliveryView::of).toList();
    }

    private static boolean subscribed(WebhookEndpoint ep, String eventType) {
        String types = ep.getEventTypes();
        if (types == null || types.isBlank() || types.trim().equals("*")) return true;
        for (String t : types.split(",")) {
            if (t.trim().equalsIgnoreCase(eventType)) return true;
        }
        return false;
    }
}
