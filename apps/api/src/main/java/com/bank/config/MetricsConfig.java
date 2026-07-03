package com.bank.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Custom Micrometer business meters. */
@Configuration
public class MetricsConfig {

    @Bean
    Counter transfersCompleted(MeterRegistry registry) {
        return Counter.builder("transfers.completed")
                .description("Number of successfully posted transfers")
                .register(registry);
    }

    @Bean
    Counter kycVerified(MeterRegistry registry) {
        return Counter.builder("kyc.verified")
                .description("Number of KYC cases reaching VERIFIED status")
                .register(registry);
    }

    @Bean
    Counter fraudAlertsCreated(MeterRegistry registry) {
        return Counter.builder("fraud.alerts.created")
                .description("Number of fraud alerts raised")
                .register(registry);
    }
}
