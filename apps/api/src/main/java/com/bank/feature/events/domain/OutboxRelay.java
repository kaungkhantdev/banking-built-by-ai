package com.bank.feature.events.domain;

import com.bank.config.RabbitTopologyConfig;
import com.bank.feature.events.persistence.OutboxEvent;
import com.bank.feature.events.persistence.OutboxEventRepository;
import com.bank.feature.events.persistence.OutboxStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls the outbox and publishes NEW rows to RabbitMQ, carrying the event id as
 * the message id so consumers can dedup. At-least-once delivery; idempotent
 * consumers make it behave like exactly-once.
 */
@Component
public class OutboxRelay {

    private final OutboxEventRepository repo;
    private final RabbitTemplate rabbit;

    public OutboxRelay(OutboxEventRepository repo, RabbitTemplate rabbit) {
        this.repo = repo;
        this.rabbit = rabbit;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.delay-ms:1000}")
    @Transactional
    public void publishBatch() {
        List<OutboxEvent> batch =
                repo.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.NEW);
        for (OutboxEvent e : batch) {
            rabbit.convertAndSend(RabbitTopologyConfig.EXCHANGE, e.getType(), e.getPayload(),
                    m -> {
                        m.getMessageProperties().setMessageId(e.getId().toString());
                        m.getMessageProperties().setContentType("application/json");
                        return m;
                    });
            e.markPublished();
        }
    }
}
