package com.bank.feature.webhooks.domain;

import com.bank.feature.webhooks.persistence.WebhookEndpoint;
import com.bank.feature.webhooks.persistence.WebhookEndpointRepository;
import com.bank.feature.webhooks.web.dto.WebhookView;
import com.bank.shared.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DefaultWebhookService implements WebhookService {

    private final WebhookEndpointRepository endpoints;

    public DefaultWebhookService(WebhookEndpointRepository endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    @Transactional
    public WebhookView register(UUID ownerUserId, String url, String secret, String eventTypes) {
        // Stub: store secret as-is; production should encrypt with AES-256
        WebhookEndpoint ep = endpoints.save(new WebhookEndpoint(ownerUserId, url, secret, eventTypes));
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
}
