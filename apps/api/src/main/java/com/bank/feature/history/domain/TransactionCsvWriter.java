package com.bank.feature.history.domain;

import com.bank.feature.ledger.persistence.Direction;
import com.bank.feature.ledger.persistence.LedgerEntry;
import com.bank.feature.ledger.persistence.LedgerEntryRepository;
import com.bank.shared.utils.Money;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Streams a wallet's ledger into CSV bytes without materializing the whole
 * result set: rows arrive via a cursor and each is detached after use so the
 * persistence context stays flat. Separate bean so its {@code @Transactional}
 * boundary applies when called from the async generator.
 */
@Component
public class TransactionCsvWriter {

    private static final String HEADER =
            "postedAt,transactionId,direction,amount,currency,runningBalance,memo\n";

    private final LedgerEntryRepository ledger;

    @PersistenceContext
    private EntityManager em;

    public TransactionCsvWriter(LedgerEntryRepository ledger) {
        this.ledger = ledger;
    }

    public record Csv(byte[] bytes, long rowCount) {}

    @Transactional(readOnly = true)
    public Csv build(UUID walletId, Direction direction, String currency, Instant from, Instant to) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Opening balance so runningBalance is absolute even with a `from` filter.
        BigDecimal running = (from == null)
                ? BigDecimal.ZERO
                : Money.of(ledger.signedSumAsOf(walletId, from.minusNanos(1)));
        long count = 0;

        try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             Stream<LedgerEntry> rows = ledger.streamSearch(walletId, direction, currency, from, to)) {
            w.write(HEADER);
            Iterator<LedgerEntry> it = rows.iterator();
            while (it.hasNext()) {
                LedgerEntry e = it.next();
                BigDecimal signed = e.getDirection() == Direction.CREDIT
                        ? e.getAmount() : e.getAmount().negate();
                running = Money.of(running.add(signed));
                w.write(e.getPostedAt() + "," + e.getTransactionId() + "," + e.getDirection() + ","
                        + e.getAmount().toPlainString() + "," + e.getCurrency() + ","
                        + running.toPlainString() + "," + csvEscape(e.getMemo()) + "\n");
                em.detach(e);   // keep the persistence context from growing
                count++;
            }
            w.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Csv(out.toByteArray(), count);
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
