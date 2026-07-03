package com.bank.feature.transfers.domain;

import com.bank.feature.compliance.domain.ComplianceGate;
import com.bank.feature.events.domain.EventPublisher;
import com.bank.feature.fees.domain.FeeEngine;
import com.bank.feature.fees.domain.FeeResult;
import com.bank.feature.fraud.domain.FraudEngine;
import com.bank.feature.kyc.domain.KycGate;
import com.bank.feature.ledger.domain.LedgerService;
import com.bank.feature.limits.domain.LimitGate;
import com.bank.feature.transfers.persistence.IdempotencyRecord;
import com.bank.feature.transfers.persistence.IdempotencyRepository;
import com.bank.feature.wallets.domain.WalletService;
import com.bank.feature.wallets.persistence.Wallet;
import com.bank.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Money-core invariants, infra-free (pure Mockito). Proves: a valid transfer
 * posts a balanced double entry + outbox row; insufficient funds is rejected
 * before any ledger write; and a duplicate idempotency key replays rather than
 * double-charging.
 */
class DefaultTransferServiceTest {

    private IdempotencyRepository idempotency;
    private WalletService wallets;
    private LedgerService ledger;
    private KycGate kycGate;
    private EventPublisher outbox;
    private ComplianceGate compliance;
    private LimitGate limitGate;
    private FraudEngine fraudEngine;
    private FeeEngine feeEngine;
    private DefaultTransferService service;

    private final UUID fromWalletId = UUID.randomUUID();
    private final UUID toWalletId = UUID.randomUUID();
    private final UUID fromAccountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        idempotency = mock(IdempotencyRepository.class);
        wallets = mock(WalletService.class);
        ledger = mock(LedgerService.class);
        kycGate = mock(KycGate.class);
        outbox = mock(EventPublisher.class);
        compliance = mock(ComplianceGate.class);
        limitGate = mock(LimitGate.class);
        fraudEngine = mock(FraudEngine.class);
        feeEngine = mock(FeeEngine.class);
        service = new DefaultTransferService(
                idempotency, wallets, ledger, kycGate, outbox,
                compliance, limitGate, fraudEngine, feeEngine);

        // Default: no fee — total() == principal, so balance checks use the raw amount
        when(feeEngine.calculate(any(), any(), any()))
                .thenAnswer(inv -> FeeResult.noFee(inv.getArgument(0)));

        Wallet from = new Wallet(fromAccountId, "USD");
        Wallet to = new Wallet(UUID.randomUUID(), "USD");
        when(wallets.getActive(fromWalletId)).thenReturn(spyId(from, fromWalletId));
        when(wallets.getActive(toWalletId)).thenReturn(spyId(to, toWalletId));
    }

    @Test
    void postsBalancedDoubleEntryAndOutboxOnSuccess() {
        when(ledger.balanceOf(fromWalletId)).thenReturn(new BigDecimal("100.0000"));

        TransferResult result = service.transfer(new TransferCommand(
                UUID.randomUUID(), fromWalletId, toWalletId,
                new BigDecimal("25.00"), "idem-1", "rent"));

        assertThat(result.replayed()).isFalse();
        assertThat(result.status()).isEqualTo("POSTED");
        verify(ledger).postDoubleEntry(any(), eq(fromWalletId), eq(toWalletId),
                eq(new BigDecimal("25.00")), eq("USD"), any(), eq("rent"));
        verify(outbox).write(eq("transfer.completed"), any(), any());
    }

    @Test
    void rejectsInsufficientFundsBeforeAnyLedgerWrite() {
        when(ledger.balanceOf(fromWalletId)).thenReturn(new BigDecimal("10.0000"));

        assertThatThrownBy(() -> service.transfer(new TransferCommand(
                UUID.randomUUID(), fromWalletId, toWalletId,
                new BigDecimal("25.00"), "idem-2", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Balance");

        verify(ledger, never()).postDoubleEntry(any(), any(), any(), any(), any(), any(), any());
        verify(outbox, never()).write(any(), any(), any());
    }

    @Test
    void duplicateIdempotencyKeyReplaysWithoutDoubleCharging() {
        UUID priorTx = UUID.randomUUID();
        // The unique-constraint insert throws on the duplicate key...
        when(idempotency.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("dup"));
        // ...and the prior record is replayed.
        IdempotencyRecord prior = IdempotencyRecord.of("idem-dup", priorTx);
        when(idempotency.findByIdemKey("idem-dup")).thenReturn(Optional.of(prior));

        TransferResult result = service.transfer(new TransferCommand(
                UUID.randomUUID(), fromWalletId, toWalletId,
                new BigDecimal("25.00"), "idem-dup", null));

        assertThat(result.replayed()).isTrue();
        assertThat(result.transactionId()).isEqualTo(priorTx);
        verify(ledger, never()).postDoubleEntry(any(), any(), any(), any(), any(), any(), any());
    }

    /** Reflection helper: stamp a deterministic id onto a Wallet for matching. */
    private static Wallet spyId(Wallet w, UUID id) {
        try {
            var field = com.bank.shared.entity.BaseAuditEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(w, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return w;
    }
}
