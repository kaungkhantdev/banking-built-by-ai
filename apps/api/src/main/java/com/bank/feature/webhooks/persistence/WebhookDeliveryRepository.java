package com.bank.feature.webhooks.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    @Query("""
            select d from WebhookDelivery d
            where d.status = 'PENDING' and d.nextAttemptAt <= :now
            order by d.nextAttemptAt asc
            """)
    List<WebhookDelivery> findDue(@Param("now") Instant now);

    List<WebhookDelivery> findByEndpointIdOrderByCreatedAtDesc(UUID endpointId);
}
