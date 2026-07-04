package com.bank.feature.webhooks.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    List<WebhookEndpoint> findByOwnerUserId(UUID ownerUserId);

    List<WebhookEndpoint> findByActiveTrue();
}
