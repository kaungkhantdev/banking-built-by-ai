package com.bank.feature.webhooks.domain;

import com.bank.feature.webhooks.persistence.WebhookDelivery;
import com.bank.feature.webhooks.persistence.WebhookDeliveryRepository;
import com.bank.feature.webhooks.persistence.WebhookEndpoint;
import com.bank.feature.webhooks.persistence.WebhookEndpointRepository;
import com.bank.shared.utils.Aes;
import com.bank.shared.utils.Hmac;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * FR-26.3: delivers queued webhook payloads, signs them (HMAC-SHA256), and
 * retries failures with exponential backoff. External calls use bounded timeouts
 * (FR-30.1) and run inside a bulkhead (FR-30.4) so a slow endpoint can't stall
 * the worker pool.
 */
@Component
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final WebhookDeliveryRepository deliveries;
    private final WebhookEndpointRepository endpoints;
    private final Aes aes;
    private final Bulkhead bulkhead;
    private final HttpClient http;
    private final Duration requestTimeout;

    public WebhookDispatcher(WebhookDeliveryRepository deliveries,
                             WebhookEndpointRepository endpoints,
                             Aes aes,
                             BulkheadRegistry bulkheadRegistry,
                             @Value("${webhook.http.connect-timeout-ms:2000}") long connectTimeoutMs,
                             @Value("${webhook.http.request-timeout-ms:3000}") long requestTimeoutMs) {
        this.deliveries = deliveries;
        this.endpoints = endpoints;
        this.aes = aes;
        this.bulkhead = bulkheadRegistry.bulkhead("webhook");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        this.requestTimeout = Duration.ofMillis(requestTimeoutMs);
    }

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void dispatchDue() {
        List<WebhookDelivery> due = deliveries.findDue(Instant.now());
        for (WebhookDelivery d : due) {
            WebhookEndpoint ep = endpoints.findById(d.getEndpointId()).orElse(null);
            if (ep == null || !ep.isActive()) {
                d.recordFailure(null, "endpoint missing or inactive");
                continue;
            }
            try {
                int status = Bulkhead.decorateCallable(bulkhead, () -> post(ep, d)).call();
                if (status >= 200 && status < 300) {
                    d.recordSuccess(status);
                } else {
                    d.recordFailure(status, "non-2xx response");
                }
            } catch (Exception e) {
                d.recordFailure(null, e.getClass().getSimpleName() + ": " + e.getMessage());
                log.warn("[WEBHOOK] delivery {} attempt failed: {}", d.getId(), e.getMessage());
            }
        }
    }

    private int post(WebhookEndpoint ep, WebhookDelivery d) throws Exception {
        String secret = aes.decrypt(ep.getSecretEncrypted());
        String signature = Hmac.sha256Hex(secret, d.getPayload());
        HttpRequest req = HttpRequest.newBuilder(URI.create(ep.getUrl()))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("X-Event-Type", d.getEventType())
                .header("X-Signature", "sha256=" + signature)
                .POST(HttpRequest.BodyPublishers.ofString(d.getPayload()))
                .build();
        HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
        return resp.statusCode();
    }
}
