package com.bank.feature.fraud.domain;

import com.bank.feature.audit.domain.Audited;
import com.bank.feature.fraud.persistence.FraudAlert;
import com.bank.feature.fraud.persistence.FraudAlertRepository;
import com.bank.feature.fraud.web.dto.FraudAlertView;
import com.bank.shared.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DefaultFraudService implements FraudService {

    private final FraudAlertRepository alerts;

    public DefaultFraudService(FraudAlertRepository alerts) {
        this.alerts = alerts;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FraudAlertView> list(String status, Pageable pageable) {
        Page<FraudAlert> page = (status == null || status.isBlank())
                ? alerts.findAll(pageable)
                : alerts.findByStatus(status.toUpperCase(), pageable);
        return page.map(FraudAlertView::of);
    }

    @Override
    @Transactional
    @Audited(action = "fraud:approve")
    public FraudAlertView approve(UUID alertId) {
        return transition(alertId, "APPROVED");
    }

    @Override
    @Transactional
    @Audited(action = "fraud:dismiss")
    public FraudAlertView dismiss(UUID alertId) {
        return transition(alertId, "FALSE_POSITIVE");
    }

    private FraudAlertView transition(UUID alertId, String newStatus) {
        FraudAlert alert = alerts.findById(alertId)
                .orElseThrow(() -> new ApiException("ALERT_NOT_FOUND", "Unknown fraud alert", 404));
        alert.setStatus(newStatus);
        return FraudAlertView.of(alert);
    }
}
