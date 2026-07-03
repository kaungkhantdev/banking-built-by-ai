package com.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Digital Banking Platform — backend entry point.
 *
 * <p>A single deployable <b>modular monolith</b>. Internally split into clean
 * feature modules under {@code com.bank.feature}, each layered into
 * {@code web} / {@code domain} / {@code persistence}. Money moves synchronously
 * inside one ACID transaction; side-effects fan out via the transactional outbox
 * (see {@code com.bank.feature.events}).
 */
@SpringBootApplication
@EnableScheduling   // outbox relay (@Scheduled)
@EnableAsync        // KYC vendor orchestration (@Async)
public class BankApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankApplication.class, args);
    }
}
