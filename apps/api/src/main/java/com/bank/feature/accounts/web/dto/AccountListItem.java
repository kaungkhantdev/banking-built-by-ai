package com.bank.feature.accounts.web.dto;

import com.bank.feature.accounts.persistence.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record AccountListItem(UUID id, UUID ownerUserId, String ownerEmail, AccountStatus status, Instant createdAt) {
}
