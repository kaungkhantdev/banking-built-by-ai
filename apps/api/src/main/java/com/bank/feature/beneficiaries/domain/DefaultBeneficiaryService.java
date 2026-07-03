package com.bank.feature.beneficiaries.domain;

import com.bank.feature.beneficiaries.persistence.Beneficiary;
import com.bank.feature.beneficiaries.persistence.BeneficiaryRepository;
import com.bank.feature.beneficiaries.web.dto.BeneficiaryView;
import com.bank.shared.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DefaultBeneficiaryService implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaries;

    public DefaultBeneficiaryService(BeneficiaryRepository beneficiaries) {
        this.beneficiaries = beneficiaries;
    }

    @Override
    @Transactional
    public BeneficiaryView create(UUID ownerUserId, String alias, UUID destinationWalletId) {
        if (beneficiaries.existsByOwnerUserIdAndDestinationWalletId(ownerUserId, destinationWalletId)) {
            throw new ApiException("BENEFICIARY_DUPLICATE",
                    "A beneficiary for this wallet already exists", 409);
        }
        return BeneficiaryView.of(beneficiaries.save(new Beneficiary(ownerUserId, alias, destinationWalletId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BeneficiaryView> list(UUID ownerUserId, Pageable pageable) {
        return beneficiaries.findByOwnerUserId(ownerUserId, pageable).map(BeneficiaryView::of);
    }

    @Override
    @Transactional
    public BeneficiaryView update(UUID id, UUID ownerUserId, String alias) {
        Beneficiary b = require(id);
        assertOwner(b, ownerUserId);
        b.setAlias(alias);
        return BeneficiaryView.of(b);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID ownerUserId) {
        Beneficiary b = require(id);
        assertOwner(b, ownerUserId);
        beneficiaries.delete(b);
    }

    private Beneficiary require(UUID id) {
        return beneficiaries.findById(id)
                .orElseThrow(() -> new ApiException("BENEFICIARY_NOT_FOUND", "Unknown beneficiary", 404));
    }

    private void assertOwner(Beneficiary b, UUID userId) {
        if (!b.getOwnerUserId().equals(userId)) {
            throw new ApiException("FORBIDDEN", "You do not own this beneficiary", 403);
        }
    }
}
