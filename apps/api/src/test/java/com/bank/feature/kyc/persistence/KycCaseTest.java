package com.bank.feature.kyc.persistence;

import com.bank.shared.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The KYC state machine forbids illegal jumps and allows the legal path. */
class KycCaseTest {

    @Test
    void walksTheHappyPath() {
        KycCase kyc = new KycCase(UUID.randomUUID());
        assertThat(kyc.getStatus()).isEqualTo(KycStatus.CREATED);
        kyc.transitionTo(KycStatus.DOCS_SUBMITTED);
        kyc.transitionTo(KycStatus.UNDER_REVIEW);
        kyc.transitionTo(KycStatus.VERIFIED);
        assertThat(kyc.getStatus()).isEqualTo(KycStatus.VERIFIED);
    }

    @Test
    void rejectsIllegalJump() {
        KycCase kyc = new KycCase(UUID.randomUUID());
        assertThatThrownBy(() -> kyc.transitionTo(KycStatus.VERIFIED))   // CREATED -> VERIFIED
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void verifiedIsTerminal() {
        KycCase kyc = new KycCase(UUID.randomUUID());
        kyc.transitionTo(KycStatus.DOCS_SUBMITTED);
        kyc.transitionTo(KycStatus.UNDER_REVIEW);
        kyc.transitionTo(KycStatus.VERIFIED);
        assertThatThrownBy(() -> kyc.transitionTo(KycStatus.REJECTED))
                .isInstanceOf(ApiException.class);
    }
}
