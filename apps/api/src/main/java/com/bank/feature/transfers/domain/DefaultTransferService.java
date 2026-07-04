package com.bank.feature.transfers.domain;

import com.bank.feature.compliance.domain.ComplianceGate;
import com.bank.feature.events.domain.EventPublisher;
import com.bank.feature.fees.domain.FeeEngine;
import com.bank.feature.fees.domain.FeeResult;
import com.bank.feature.fraud.domain.FraudEngine;
import com.bank.feature.kyc.domain.KycGate;
import com.bank.feature.ledger.domain.LedgerService;
import com.bank.feature.ledger.persistence.Direction;
import com.bank.feature.ledger.persistence.LedgerEntry;
import com.bank.feature.limits.domain.LimitGate;
import com.bank.feature.transfers.persistence.IdempotencyRecord;
import com.bank.feature.transfers.persistence.IdempotencyRepository;
import com.bank.feature.wallets.domain.WalletService;
import com.bank.feature.wallets.persistence.Wallet;
import com.bank.shared.exception.ApiException;
import com.bank.shared.utils.Money;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The money core. {@link #transfer} runs in a single {@code @Transactional} unit:
 * (1) insert the idempotency key (unique constraint = no double-charge),
 * (2) validate wallets + currency, (3) compliance screening, (4) KYC-gate the sender,
 * (5) limit gate, (6) fraud assessment, (7) fee calculation,
 * (8) check funds against the derived balance + fee, (9) post two balanced ledger legs,
 * (10) write an outbox row — all committed together or all rolled back.
 */
@Service
public class DefaultTransferService implements TransferService {

    private static final String CUSTOMER_TIER = "STANDARD";

    private final IdempotencyRepository idempotency;
    private final WalletService wallets;
    private final LedgerService ledger;
    private final KycGate kycGate;
    private final EventPublisher outbox;
    private final ComplianceGate compliance;
    private final LimitGate limitGate;
    private final FraudEngine fraudEngine;
    private final FeeEngine feeEngine;

    public DefaultTransferService(IdempotencyRepository idempotency,
                                  WalletService wallets,
                                  LedgerService ledger,
                                  KycGate kycGate,
                                  EventPublisher outbox,
                                  ComplianceGate compliance,
                                  LimitGate limitGate,
                                  FraudEngine fraudEngine,
                                  FeeEngine feeEngine) {
        this.idempotency = idempotency;
        this.wallets = wallets;
        this.ledger = ledger;
        this.kycGate = kycGate;
        this.outbox = outbox;
        this.compliance = compliance;
        this.limitGate = limitGate;
        this.fraudEngine = fraudEngine;
        this.feeEngine = feeEngine;
    }

    @Override
    @Transactional
    public TransferResult transfer(TransferCommand cmd) {
        if (!Money.isPositive(cmd.amount())) {
            throw new ApiException("AMOUNT_INVALID", "Amount must be positive", 422);
        }

        // 1) Idempotency: insert-first. A duplicate key replays the prior result.
        try {
            idempotency.saveAndFlush(
                    IdempotencyRecord.of(cmd.idempotencyKey(), cmd.transactionId()));
        } catch (DataIntegrityViolationException duplicate) {
            UUID existing = idempotency.findByIdemKey(cmd.idempotencyKey())
                    .orElseThrow(() -> new ApiException("IDEMPOTENCY_RACE",
                            "Idempotency key in an inconsistent state", 409))
                    .getTransactionId();
            return TransferResult.replayed(existing);
        }

        Wallet from = wallets.getActive(cmd.fromWalletId());
        Wallet to = wallets.getActive(cmd.toWalletId());

        if (from.getId().equals(to.getId())) {
            throw new ApiException("SAME_WALLET", "Source and destination are identical", 422);
        }
        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new ApiException("CURRENCY_MISMATCH", "Cross-currency not allowed", 422);
        }

        // 2) Compliance screening
        compliance.screenTransfer(cmd.transactionId(), from.getAccountId(), to.getAccountId());

        // 3) KYC gate: money-out requires a verified sender.
        kycGate.assertMoneyOutAllowed(from.getAccountId());

        // 4) Limit gate: check and record daily/monthly usage
        limitGate.assertWithinLimitsAndRecord(
                from.getAccountId(), cmd.amount(), from.getCurrency(), CUSTOMER_TIER);

        // 5) Fraud assessment
        fraudEngine.assess(cmd.transactionId(), from.getAccountId(), cmd.amount());

        // 6) Fee calculation (promotional waivers may zero it — FR-19.4)
        FeeResult fee = feeEngine.calculate(from.getAccountId(), cmd.amount(),
                "STANDARD", CUSTOMER_TIER);

        // 7) Insufficient-funds check against the DERIVED balance (including fee).
        BigDecimal balance = ledger.balanceOf(from.getId());
        if (!Money.covers(balance, fee.total())) {
            throw new ApiException("INSUFFICIENT_FUNDS",
                    "Balance %s < required %s".formatted(balance, fee.total()), 422);
        }

        // 8) Double-entry: two balanced legs sharing one transactionId.
        Instant now = Instant.now();
        ledger.postDoubleEntry(cmd.transactionId(), from.getId(), to.getId(),
                fee.principal(), from.getCurrency(), now, cmd.memo());

        // 8b) Fee is its own balanced double entry: sender → fee-collection wallet
        //     (FR-19.3). Shares the transactionId so a reversal refunds it too.
        if (Money.isPositive(fee.fee())) {
            UUID feeWallet = wallets.systemFeeWalletId(from.getCurrency());
            ledger.postDoubleEntry(cmd.transactionId(), from.getId(), feeWallet,
                    fee.fee(), from.getCurrency(), now, "fee:" + cmd.transactionId());
        }

        // 9) Outbox row in the SAME transaction — fans out after commit.
        outbox.write("transfer.completed", cmd.transactionId(), Map.of(
                "from", from.getId().toString(),
                "to", to.getId().toString(),
                "amount", Money.of(fee.principal()).toString(),
                "fee", fee.fee().toString(),
                "currency", from.getCurrency()));

        return TransferResult.posted(cmd.transactionId());
    }

    @Override
    @Transactional
    public TransferResult reverse(UUID originalTxId, String reason) {
        List<LedgerEntry> legs = ledger.entriesOf(originalTxId);
        if (legs.isEmpty()) {
            throw new ApiException("TX_NOT_FOUND", "Unknown transaction", 404);
        }

        UUID reversalTxId = UUID.randomUUID();
        Instant now = Instant.now();
        for (LedgerEntry e : legs) {
            Direction opposite = e.getDirection() == Direction.DEBIT
                    ? Direction.CREDIT : Direction.DEBIT;
            ledger.postLeg(reversalTxId, e.getWalletId(), opposite, e.getAmount(),
                    e.getCurrency(), now, "reversal:" + reason);
        }
        outbox.write("transfer.reversed", reversalTxId, Map.of(
                "original", originalTxId.toString(),
                "reason", reason));
        return TransferResult.posted(reversalTxId);
    }
}
