package com.bank.feature.transfers.domain;

import java.util.UUID;

/** Port: the money core. Both methods run in a single ACID transaction. */
public interface TransferService {

    TransferResult transfer(TransferCommand command);

    /** Reverse a prior transaction with compensating entries (never a delete). */
    TransferResult reverse(UUID originalTransactionId, String reason);
}
