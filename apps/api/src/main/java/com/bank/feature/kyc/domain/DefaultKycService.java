package com.bank.feature.kyc.domain;

import com.bank.feature.kyc.persistence.KycCase;
import com.bank.feature.kyc.persistence.KycCaseRepository;
import com.bank.feature.kyc.persistence.KycStatus;
import com.bank.feature.kyc.web.dto.KycCaseView;
import com.bank.shared.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * KYC orchestration. Document submission stores only a reference (the bytes go to
 * object storage in production) and hands verification to an async worker so the
 * request never blocks on the vendor. The case advances through guarded states.
 */
@Service
public class DefaultKycService implements KycService {

    private final KycCaseRepository cases;
    private final KycVendorClient vendor;

    public DefaultKycService(KycCaseRepository cases, KycVendorClient vendor) {
        this.cases = cases;
        this.vendor = vendor;
    }

    @Override
    @Transactional
    public UUID openCase(UUID accountId) {
        return cases.findByAccountId(accountId)
                .map(KycCase::getId)
                .orElseGet(() -> cases.save(new KycCase(accountId)).getId());
    }

    @Override
    @Transactional
    public void submitDocument(UUID accountId, byte[] documentBytes, String contentType) {
        KycCase kyc = cases.findByAccountId(accountId)
                .orElseThrow(() -> new ApiException("KYC_NOT_FOUND", "No KYC case for account", 404));
        // In production: docStore.put(documentBytes, contentType) -> store the returned ref.
        kyc.transitionTo(KycStatus.DOCS_SUBMITTED);
        cases.save(kyc);
        orchestrateAsync(kyc.getId());
    }

    @Async
    @Transactional
    public void orchestrateAsync(UUID caseId) {
        cases.findById(caseId).ifPresent(kyc -> {
            kyc.transitionTo(KycStatus.UNDER_REVIEW);
            cases.save(kyc);
            KycVendorClient.VendorResult r = vendor.verify(kyc.getAccountId());
            applyVendorResult(caseId, r);
        });
    }

    @Transactional
    public void applyVendorResult(UUID caseId, KycVendorClient.VendorResult r) {
        cases.findById(caseId).ifPresent(kyc -> {
            kyc.setVendorRef(r.reference());
            kyc.transitionTo(r.approved() ? KycStatus.VERIFIED : KycStatus.REJECTED);
            if (!r.approved()) {
                kyc.setRejectReason(r.reason());
            }
            cases.save(kyc);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public KycStatus statusOf(UUID accountId) {
        return cases.findByAccountId(accountId)
                .map(KycCase::getStatus)
                .orElse(KycStatus.CREATED);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KycCaseView> list(KycStatus status, Pageable pageable) {
        Page<KycCase> page = status != null
                ? cases.findByStatus(status, pageable)
                : cases.findAll(pageable);
        return page.map(c -> new KycCaseView(
                c.getId(), c.getAccountId(), c.getStatus(),
                c.getVendorRef(), c.getRejectReason(), c.getUpdatedAt()));
    }
}
