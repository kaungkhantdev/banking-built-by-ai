package com.bank.feature.fraud.domain;

import com.bank.feature.fraud.web.dto.FraudAlertView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Operator-facing management of fraud alerts raised by {@link FraudEngine}. */
public interface FraudService {

    Page<FraudAlertView> list(String status, Pageable pageable);

    /** Clear a held/flagged alert so its transfer may proceed (status → APPROVED). */
    FraudAlertView approve(UUID alertId);

    /** Dismiss an alert as noise (status → FALSE_POSITIVE). */
    FraudAlertView dismiss(UUID alertId);
}
