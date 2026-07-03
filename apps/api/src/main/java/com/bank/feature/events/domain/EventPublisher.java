package com.bank.feature.events.domain;

import java.util.Map;
import java.util.UUID;

/**
 * Port: publish a domain event. The production implementation
 * ({@link OutboxWriter}) writes to the transactional outbox; tests can supply an
 * in-memory alternate without changing callers.
 */
public interface EventPublisher {

    void write(String type, UUID aggregateId, Map<String, Object> payload);
}
