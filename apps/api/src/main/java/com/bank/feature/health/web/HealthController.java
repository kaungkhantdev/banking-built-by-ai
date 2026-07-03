package com.bank.feature.health.web;

import com.bank.feature.events.persistence.OutboxEventRepository;
import com.bank.feature.events.persistence.OutboxStatus;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

/**
 * Operator-facing system-health summary consumed by the web console's Home page
 * (GET /v1/admin/health). Distinct from Spring Actuator's /actuator/health:
 * this returns a compact, domain-shaped view (db, broker, outbox lag) rather
 * than Actuator's generic component tree.
 *
 * Gated by {@code authenticated()} only (see SecurityConfig#anyRequest) — any
 * signed-in operator may read it; there is no dedicated health permission.
 */
@RestController
@RequestMapping("/v1/admin")
public class HealthController {

    /** Wire-format contract mirrored by the web client's HealthSummary type. */
    public record HealthSummary(String status, String db, String broker, long outboxLagSeconds) {
    }

    private final DataSource dataSource;
    private final ConnectionFactory rabbitConnectionFactory;
    private final OutboxEventRepository outbox;

    public HealthController(DataSource dataSource,
                            ConnectionFactory rabbitConnectionFactory,
                            OutboxEventRepository outbox) {
        this.dataSource = dataSource;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
        this.outbox = outbox;
    }

    @GetMapping("/health")
    public HealthSummary health() {
        boolean dbUp = checkDb();
        boolean brokerUp = checkBroker();
        long lag = dbUp ? outboxLagSeconds() : 0L;

        // DB is the system of record: if it's down the platform is DOWN.
        // A down broker only degrades (writes still commit; events queue in the outbox).
        String overall;
        if (!dbUp) {
            overall = "DOWN";
        } else if (!brokerUp) {
            overall = "DEGRADED";
        } else {
            overall = "UP";
        }

        return new HealthSummary(overall, dbUp ? "UP" : "DOWN", brokerUp ? "UP" : "DOWN", lag);
    }

    private boolean checkDb() {
        try (var conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean checkBroker() {
        try (Connection conn = rabbitConnectionFactory.createConnection()) {
            return conn.isOpen();
        } catch (Exception e) {
            return false;
        }
    }

    /** Age (seconds) of the oldest unpublished outbox row; 0 when the relay is caught up. */
    private long outboxLagSeconds() {
        return outbox.findFirstByStatusOrderByCreatedAtAsc(OutboxStatus.NEW)
                .map(e -> Math.max(0L, Duration.between(e.getCreatedAt(), Instant.now()).getSeconds()))
                .orElse(0L);
    }
}
