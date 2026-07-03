package com.bank.feature.webhooks.domain;

import com.bank.feature.webhooks.web.dto.WebhookView;

import java.util.List;
import java.util.UUID;

public interface WebhookService {

    WebhookView register(UUID ownerUserId, String url, String secret, String eventTypes);

    List<WebhookView> list(UUID ownerUserId);

    void deregister(UUID id, UUID ownerUserId);
}
