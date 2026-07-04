package com.bank.feature.history.domain;

import com.bank.feature.history.web.dto.TransactionView;
import com.bank.feature.ledger.persistence.Direction;
import com.bank.feature.ledger.persistence.LedgerEntry;
import com.bank.feature.ledger.persistence.LedgerEntryRepository;
import com.bank.shared.utils.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultHistoryService implements HistoryService {

    private final LedgerEntryRepository ledger;

    public DefaultHistoryService(LedgerEntryRepository ledger) {
        this.ledger = ledger;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionView> listForWallet(UUID walletId, String direction, String currency,
                                               Instant from, Instant to, Pageable pageable) {
        Direction dir = parseDirection(direction);
        String ccy = (currency == null || currency.isBlank()) ? null : currency.toUpperCase();
        return ledger.search(walletId, dir, ccy, from, to, pageable)
                .map(e -> TransactionView.of(e, ledger.signedSumAsOf(walletId, e.getPostedAt())));
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCsv(UUID walletId, String direction, String currency,
                            Instant from, Instant to) {
        Direction dir = parseDirection(direction);
        String ccy = (currency == null || currency.isBlank()) ? null : currency.toUpperCase();
        List<LedgerEntry> rows = ledger.searchAll(walletId, dir, ccy, from, to);

        StringBuilder csv = new StringBuilder(
                "postedAt,transactionId,direction,amount,currency,runningBalance,memo\n");
        BigDecimal running = BigDecimal.ZERO;   // rows are ascending by postedAt
        for (LedgerEntry e : rows) {
            BigDecimal signed = e.getDirection() == Direction.CREDIT
                    ? e.getAmount() : e.getAmount().negate();
            running = Money.of(running.add(signed));
            csv.append(e.getPostedAt()).append(',')
                    .append(e.getTransactionId()).append(',')
                    .append(e.getDirection()).append(',')
                    .append(e.getAmount().toPlainString()).append(',')
                    .append(e.getCurrency()).append(',')
                    .append(running.toPlainString()).append(',')
                    .append(csvEscape(e.getMemo())).append('\n');
        }
        return csv.toString();
    }

    private static Direction parseDirection(String direction) {
        return (direction == null || direction.isBlank())
                ? null : Direction.valueOf(direction.toUpperCase());
    }

    /** Quote fields containing commas/quotes/newlines per RFC 4180. */
    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
