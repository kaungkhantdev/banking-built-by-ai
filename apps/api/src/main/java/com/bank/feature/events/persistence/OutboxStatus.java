package com.bank.feature.events.persistence;

/** Outbox row lifecycle: written NEW inside the business TX, flipped PUBLISHED by the relay. */
public enum OutboxStatus {
    NEW, PUBLISHED
}
