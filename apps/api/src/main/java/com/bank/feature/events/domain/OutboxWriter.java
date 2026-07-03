package com.bank.feature.events.domain;

import com.bank.feature.events.persistence.OutboxEvent;
import com.bank.feature.events.persistence.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * The production {@link EventPublisher}: writes an outbox row inside the
 * caller's transaction ({@code MANDATORY}) so the event commits atomically with
 * the business change. No broker call here — the relay publishes after commit.
 */
@Component
public class OutboxWriter implements EventPublisher {

    private final OutboxEventRepository repo;
    private final ObjectMapper mapper;

    public OutboxWriter(OutboxEventRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String type, UUID aggregateId, Map<String, Object> payload) {
        repo.save(new OutboxEvent(type, aggregateId.toString(), serialize(payload)));
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize outbox payload", ex);
        }
    }
}
